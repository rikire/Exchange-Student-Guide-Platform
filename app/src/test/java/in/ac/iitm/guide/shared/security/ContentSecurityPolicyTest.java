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
 * and a static file. ADR-0022 adds {@code blob:} images and workers and nothing else.
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

    /**
     * ADR-0022 widened the policy by exactly two sources, for the file field's image preview: a
     * {@code blob:} image and a {@code blob:} worker. Pinned whole, so nothing else widens it unnoticed.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/", "/submit"})
    // trace:FR-010
    void the_policy_is_exactly_the_strict_one_plus_blob_images_and_workers(String path) throws Exception {
        var policy = mockMvc.perform(get(path)).andReturn().getResponse().getHeader("Content-Security-Policy");

        assertThat(policy)
                .isEqualTo("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' blob:;"
                        + " worker-src blob:; font-src 'self'; connect-src 'self'; media-src 'self';"
                        + " object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'");
    }
}
