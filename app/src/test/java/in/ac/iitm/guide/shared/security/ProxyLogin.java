package in.ac.iitm.guide.shared.security;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * One browser behind a proxy, for the tests of DEBT-014: its own cookies, and the client address the
 * proxy reports for it in {@code X-Forwarded-For}. Over a real connection, since the Tomcat valve that
 * reads the header is not part of MockMvc.
 */
final class ProxyLogin {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    private final HttpClient http =
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    // Kept by hand: behind an https proxy the cookies are Secure, and the JDK's cookie store will not
    // send a Secure cookie over the test's plain http, where a browser talking to the proxy would.
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private final String base;
    private final String clientAddress;

    ProxyLogin(int port, String clientAddress) {
        this.base = "http://localhost:" + port;
        this.clientAddress = clientAddress;
    }

    /** Opens the login page, then sends the password with the token it carried. */
    HttpResponse<String> logIn(String password) throws IOException, InterruptedException {
        var page = send(request("/moderate/login").GET().build());
        var token = CSRF.matcher(page.body());
        if (!token.find()) {
            throw new AssertionError("the login page carries no CSRF token: " + page.body());
        }
        var form = "_csrf=" + URLEncoder.encode(token.group(1), StandardCharsets.UTF_8) + "&password="
                + URLEncoder.encode(password, StandardCharsets.UTF_8);
        return send(request("/moderate/login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build());
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        response.headers().allValues("Set-Cookie").forEach(header -> {
            var pair = header.split(";", 2)[0].split("=", 2);
            cookies.put(pair[0], pair.length > 1 ? pair[1] : "");
        });
        return response;
    }

    private HttpRequest.Builder request(String path) {
        var builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("X-Forwarded-For", clientAddress)
                .header("X-Forwarded-Proto", "https");
        if (!cookies.isEmpty()) {
            var header = new StringBuilder();
            cookies.forEach((name, value) -> header.append(header.isEmpty() ? "" : "; ")
                    .append(name)
                    .append('=')
                    .append(value));
            builder.header("Cookie", header.toString());
        }
        return builder;
    }
}
