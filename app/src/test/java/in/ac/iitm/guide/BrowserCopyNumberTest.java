package in.ac.iitm.guide;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Playwright;
import in.ac.iitm.guide.shared.persistence.Submission;
import in.ac.iitm.guide.shared.persistence.SubmissionStatus;
import in.ac.iitm.guide.shared.persistence.SubmissionType;
import jakarta.persistence.EntityManager;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Fix 2.4 in a real browser, under the strict Content-Security-Policy: the confirmation's Copy button
 * puts the number, hyphens included, on the clipboard. Playwright grants the clipboard on localhost,
 * a secure context; the plain-HTTP fallback (select and say Ctrl+C) is the human's check on the stand.
 * Runs only under {@code -P browser}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BrowserCopyNumberTest {

    private static final String NUMBER = "SUB-K7M2-QX9P-4TVB";

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private final List<String> problems = new ArrayList<>();

    @BeforeAll
    void startTheBrowserAndSaveASubmission() {
        playwright = Playwright.create();
        browser = TestBrowser.launch(playwright);
        var submission = new Submission();
        submission.setSubmissionNumber(NUMBER);
        submission.setType(SubmissionType.NEW_ARTICLE);
        submission.setTitle("Getting a SIM card");
        submission.setSummary("Which shops sell one.");
        submission.setBody("Bring your passport.");
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setSubmittedAt(OffsetDateTime.now());
        submission.setTags(new HashSet<>());
        transaction.executeWithoutResult(status -> entityManager.persist(submission));
    }

    @AfterAll
    void closeTheBrowserAndClearTheDatabase() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        jdbc.execute("DELETE FROM submission_tag");
        jdbc.execute("DELETE FROM submission");
    }

    @BeforeEach
    void openAProfileThatMayUseTheClipboard() {
        problems.clear();
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
        context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    }

    @AfterEach
    void closeTheProfileAndReportWhatWentWrong() {
        context.close();
        assertThat(problems).as("policy violations and script errors").isEmpty();
    }

    @Test
    // trace:FR-012
    void copy_puts_the_number_on_the_clipboard_and_says_so() {
        var page = context.newPage();
        page.onConsoleMessage(message -> {
            if ("error".equals(message.type())) {
                problems.add(message.text());
            }
        });
        page.onPageError(error -> problems.add(error));
        page.navigate("http://localhost:" + port + "/submissions/" + NUMBER + "/confirmation");

        page.click("button.copy-number");

        page.locator(".copy-result").getByText("Copied").waitFor();
        assertThat(page.evaluate("navigator.clipboard.readText()")).isEqualTo(NUMBER);
    }
}
