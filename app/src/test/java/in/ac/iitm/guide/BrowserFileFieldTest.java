package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.FilePayload;
import in.ac.iitm.guide.media.MediaTestFiles;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fix 3.6's file field in a real browser, under the policy ADR-0022 widened by {@code blob:} images
 * and workers: FilePond in {@code storeAsFile} mode, with the image preview. A chosen file shows its
 * name, size and a way to remove it, a photo a preview, and the form still posts the file as
 * {@code attachment}. Every test also fails on a policy violation or a script error, which is how a
 * preview the policy blocks shows. Files are chosen through FilePond's own file input: Playwright
 * cannot drop a file from the operating system, so dropping is a person's check.
 * Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserFileFieldTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private final List<String> problems = new ArrayList<>();

    @BeforeAll
    void startTheBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    void closeTheBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void openAProfile() {
        problems.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() throws IOException {
        context.close();
        jdbc.execute("DELETE FROM media_asset");
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
        MediaTestFiles.empty(MediaTestFiles.ROOT);
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-010
    void a_chosen_file_shows_its_name_and_size() {
        var page = openTheForm();

        choose(page, "handbook.pdf", "application/pdf", MediaTestFiles.pdf(30_000));

        assertThat(page.textContent(".filepond--file-info-main")).isEqualTo("handbook.pdf");
        assertThat(page.textContent(".filepond--file-info-sub")).isEqualTo("30 KB");
    }

    @Test
    // trace:FR-010
    void a_chosen_photo_is_previewed() {
        var page = openTheForm();

        choose(page, "hostel.jpg", "image/jpeg", MediaTestFiles.jpeg(640, 480));

        page.waitForSelector(".filepond--image-preview canvas");
    }

    @Test
    // trace:FR-010
    void a_chosen_file_can_be_removed_before_sending() {
        var page = openTheForm();
        choose(page, "handbook.pdf", "application/pdf", MediaTestFiles.pdf(30_000));

        page.click(".filepond--action-remove-item");

        page.waitForSelector(
                ".filepond--item",
                new Page.WaitForSelectorOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.DETACHED));
    }

    @Test
    // trace:FR-010
    void the_chosen_file_is_sent_with_the_form_and_stored_with_the_submission() {
        var page = openTheForm();
        page.fill("input[name=title]", "Hostel rooms");
        page.fill("input[name=summary]", "A summary.");
        page.evaluate("() => document.querySelector('.CodeMirror').CodeMirror.setValue('Rooms are shared.')");
        choose(page, "hostel.jpg", "image/jpeg", MediaTestFiles.jpeg(640, 480));

        page.click("button[type=submit]");
        page.waitForURL("**/confirmation");

        assertThat(jdbc.queryForObject(
                        "SELECT m.original_name FROM media_asset m JOIN submission s ON s.id = m.submission_id"
                                + " WHERE s.title = 'Hostel rooms'",
                        String.class))
                .isEqualTo("hostel.jpg");
    }

    @Test
    // trace:FR-010
    void a_form_sent_with_no_file_is_submitted_without_an_attachment() {
        var page = openTheForm();
        page.fill("input[name=title]", "No file");
        page.fill("input[name=summary]", "A summary.");
        page.evaluate("() => document.querySelector('.CodeMirror').CodeMirror.setValue('Text.')");

        page.click("button[type=submit]");
        page.waitForURL("**/confirmation");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_asset", Integer.class))
                .isZero();
    }

    private Page openTheForm() {
        var page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
        page.navigate("http://localhost:" + port + "/submit");
        page.waitForSelector(".filepond--root");
        return page;
    }

    private static void choose(Page page, String name, String type, byte[] content) {
        page.setInputFiles(".filepond--browser", new FilePayload(name, type, content));
        page.waitForSelector(".filepond--item");
    }
}
