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
                .filter(route -> route.contains(" /moderate/"))
                // The login, and the logout, which ends whatever session there is and gates nothing.
                .filter(route -> !route.endsWith("/moderate/login") && !route.endsWith("/moderate/logout"))
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

    @Test
    // trace:FR-014
    void logging_out_ends_the_session_and_the_login_page_says_so() throws Exception {
        var loggedIn = logIn(new MockHttpSession(), PASSWORD);
        var session = (MockHttpSession) loggedIn.getRequest().getSession();
        var queue = mockMvc.perform(get("/moderate/queue")
                        .session(session)
                        .cookie(loggedIn.getResponse().getCookies()))
                .andReturn();
        assertThat(queue.getResponse().getContentAsString())
                .containsPattern("<form[^>]*action=\"/moderate/logout\"")
                .contains("Log out");

        var loggedOut = mockMvc.perform(post("/moderate/logout")
                        .session(session)
                        .param("_csrf", token(queue))
                        .cookie(queue.getResponse().getCookies()))
                .andReturn();

        assertThat(loggedOut.getResponse().getRedirectedUrl()).isEqualTo("/moderate/login?logout");
        assertThat(mockMvc.perform(get("/moderate/login?logout"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString())
                .contains("You are logged out");
        assertThat(mockMvc.perform(get("/moderate/queue").session(session))
                        .andReturn()
                        .getResponse()
                        .getRedirectedUrl())
                .as("the old session opens nothing")
                .endsWith("/moderate/login");
    }

    @Test
    // trace:FR-014
    void a_logout_without_the_csrf_token_is_refused_and_the_session_stays() throws Exception {
        var loggedIn = logIn(new MockHttpSession(), PASSWORD);
        var session = (MockHttpSession) loggedIn.getRequest().getSession();

        var refused = mockMvc.perform(post("/moderate/logout").session(session)).andReturn();

        assertThat(refused.getResponse().getStatus()).isEqualTo(403);
        assertThat(mockMvc.perform(get("/moderate/queue").session(session))
                        .andReturn()
                        .getResponse()
                        .getStatus())
                .isEqualTo(200);
    }

    @Test
    // trace:FR-014
    void logging_in_replaces_the_csrf_token_the_browser_held_before() throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var before = form.getResponse().getCookie("XSRF-TOKEN");
        assertThat(before).as("the login form sets the token cookie").isNotNull();

        var loggedIn = mockMvc.perform(post("/moderate/login")
                        .session(session)
                        .param("_csrf", token(form))
                        .param("password", PASSWORD)
                        .cookie(form.getResponse().getCookies()))
                .andReturn();

        var after = loggedIn.getResponse().getCookie("XSRF-TOKEN");
        assertThat(after).as("the login answers with the token cookie replaced").isNotNull();
        assertThat(after.getValue()).isNotEqualTo(before.getValue());
        // The browser now holds the replaced cookie, so the token known before login no longer matches it.
        var withOldToken = post("/moderate/logout")
                .session((MockHttpSession) loggedIn.getRequest().getSession())
                .param("_csrf", token(form));
        if (!after.getValue().isEmpty()) {
            withOldToken.cookie(after);
        }
        assertThat(mockMvc.perform(withOldToken).andReturn().getResponse().getStatus())
                .isEqualTo(403);
    }

    @Test
    // trace:FR-014
    void the_login_page_leads_back_to_the_guide() throws Exception {
        var page = mockMvc.perform(get("/moderate/login"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(page.substring(page.indexOf("<main")))
                .contains("Back to the guide")
                .contains("href=\"/\"");
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
