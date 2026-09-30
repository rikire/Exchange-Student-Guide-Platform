package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

/**
 * security.md, "The admin area": with no hash in {@code GUIDE_ADMIN_PASSWORD_HASH}, no password logs
 * in. A stand started without the variable must not have an open moderation queue.
 */
@SpringBootTest(properties = "guide.admin.password-hash=")
@AutoConfigureMockMvc
class ModeratorLoginWithoutHashTest {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"", "password", "moderator"})
    // trace:FR-014
    void no_password_logs_in_when_no_hash_is_set(String password) throws Exception {
        var session = new MockHttpSession();
        var form = mockMvc.perform(get("/moderate/login").session(session)).andReturn();
        var token = CSRF.matcher(form.getResponse().getContentAsString());
        assertThat(token.find()).as("the login form carries a CSRF token").isTrue();

        var result = mockMvc.perform(post("/moderate/login")
                        .session(session)
                        .cookie(form.getResponse().getCookies())
                        .param("_csrf", token.group(1))
                        .param("password", password))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }
}
