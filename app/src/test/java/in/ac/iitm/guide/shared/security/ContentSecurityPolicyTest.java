package in.ac.iitm.guide.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * ADR-0013's strict policy: scripts and styles only from our own files, nothing inline, sent by every
 * response rather than only the editor's form — including a not-found page, a redirect to the login
 * and a static file.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContentSecurityPolicyTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/submit", "/articles/no-such-article", "/moderate/queue", "/css/site.css"})
    // trace:FR-027
    void every_response_allows_scripts_and_styles_only_from_the_site_itself(String path) throws Exception {
        var policy = mockMvc.perform(get(path)).andReturn().getResponse().getHeader("Content-Security-Policy");

        assertThat(policy)
                .as("the policy of %s", path)
                .contains("default-src 'self'")
                .contains("script-src 'self'")
                .contains("style-src 'self'")
                .contains("object-src 'none'")
                .contains("frame-ancestors 'none'")
                .doesNotContain("unsafe-inline")
                .doesNotContain("unsafe-eval");
    }
}
