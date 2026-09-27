package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import in.ac.iitm.guide.RouteContractTest;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** ADR-0009's gate in front of {@code /moderate/**}: one shared password, checked against its hash. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ModeratorLoginTest {

    private static final String PASSWORD = "the office's password";
    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    // trace:FR-014
    void every_moderate_route_but_the_login_redirects_to_the_login_without_a_session() throws Exception {
        // Enumerated from the route contract, not by hand, so a moderate route added later is covered
        // without anyone remembering to add it here (roadmap phase 3, the admin-route check).
        var gated = RouteContractTest.builtRoutes().stream()
                .filter(route -> route.contains(" /moderate/") && !route.endsWith("/moderate/login"))
                .toList();
        assertThat(gated).as("the moderate routes are in the contract").isNotEmpty();

        var session = new MockHttpSession();
        var loginPage = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        for (var route : gated) {
            var method = route.substring(0, route.indexOf(' '));
            var path = route.substring(route.indexOf(' ') + 1).replace("{}", "SUB-TEST-0000-0001");
            var request = "GET".equals(method)
                    ? get(path).session(session)
                    : post(path).session(session).param("_csrf", token(loginPage));
            if (loginPage.getResponse().getCookies().length > 0) {
                request.cookie(loginPage.getResponse().getCookies());
            }

            var result = mockMvc.perform(request).andReturn();

            assertThat(result.getResponse().getStatus()).as(route).isEqualTo(302);
            assertThat(result.getResponse().getRedirectedUrl()).as(route).endsWith("/moderate/login");
        }
    }

    @Test
    // trace:FR-014
    void a_wrong_password_answers_401_on_the_form_and_is_logged_as_a_warning(CapturedOutput output) throws Exception {
        var result = logIn(new MockHttpSession(), "not the password");

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .contains("name=\"password\"")
                .contains("That password is not right");
        assertThat(output).contains("WARN").contains("Failed moderator login").doesNotContain("not the password");
        assertThat(mockMvc.perform(get("/moderate/queue")
                                .session((MockHttpSession) result.getRequest().getSession()))
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl())
                .as("a wrong password opens nothing")
                .endsWith("/moderate/login");
    }

    @Test
    // trace:FR-014
    void the_right_password_reaches_the_queue_under_a_new_session_id() throws Exception {
        var session = new MockHttpSession();
        var before = session.getId();

        var result = logIn(session, PASSWORD);

        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/moderate/queue");
        var loggedIn = (MockHttpSession) result.getRequest().getSession();
        assertThat(loggedIn.getId()).as("the session id changes on login").isNotEqualTo(before);
        assertThat(mockMvc.perform(get("/moderate/queue").session(loggedIn))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(200);
    }

    private MvcResult logIn(MockHttpSession session, String password) throws Exception {
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        assertThat(form.getResponse().getStatus()).isEqualTo(200);
        var request = post("/moderate/login")
                .session(session)
                .param("_csrf", token(form))
                .param("password", password);
        if (form.getResponse().getCookies().length > 0) {
            request.cookie(form.getResponse().getCookies());
        }
        return mockMvc.perform(request).andReturn();
    }

    private static String token(MvcResult page) throws Exception {
        var matcher = CSRF.matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).as("the page carries a CSRF token").isTrue();
        return matcher.group(1);
    }
}
