package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The moderator's form for publishing directly (FEAT-021) has the contributor's editor and file
 * field: the editor across the page frame, and the drop zone rather than the browser's own button.
 * Seen on 10 Oct: it kept the narrow editor and the bare file input. Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserPublishFormTest {

    private static final String PASSWORD = "the office's password";

    @DynamicPropertySource
    static void password(DynamicPropertyRegistry registry) {
        registry.add("guide.admin.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;

    @BeforeAll
    void startTheBrowser() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
    }

    @AfterAll
    void closeTheBrowser() {
        browser.close();
        playwright.close();
    }

    @Test
    // trace:FR-023
    void on_a_wide_screen_the_moderators_editor_takes_the_width_of_the_page_frame() {
        var page = openTheForm();

        var editor = page.locator(".EasyMDEContainer").boundingBox();

        assertThat(editor.width).as("the editor's width at 1920 px").isGreaterThanOrEqualTo(0.6 * 1920);
    }

    @Test
    // trace:FR-023
    void the_moderators_file_field_is_the_drop_zone() {
        var page = openTheForm();

        page.waitForSelector(".filepond--root");
    }

    private Page openTheForm() {
        var page = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080))
                .newPage();
        page.navigate("http://localhost:" + port + "/moderate/login");
        page.fill("input[name=password]", PASSWORD);
        page.click("main button[type=submit]");
        page.waitForURL(url -> !url.endsWith("/moderate/login"));
        page.navigate("http://localhost:" + port + "/moderate/write");
        page.waitForSelector(".EasyMDEContainer");
        return page;
    }
}
