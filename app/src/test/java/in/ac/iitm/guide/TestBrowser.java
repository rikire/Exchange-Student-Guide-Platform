package in.ac.iitm.guide;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

/**
 * The browser every Browser*Test runs in: Playwright's own Chromium, or, with
 * {@code -Dbrowser.channel=chrome}, the Google Chrome installed on the machine, for a machine that cannot
 * download Playwright's build (docs/onboarding.md). Run with PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 so the
 * driver does not try to download it either.
 */
final class TestBrowser {

    private TestBrowser() {}

    static Browser launch(Playwright playwright) {
        var channel = System.getProperty("browser.channel");
        var options = new BrowserType.LaunchOptions();
        if (channel != null && !channel.isBlank()) {
            options.setChannel(channel);
        }
        return playwright.chromium().launch(options);
    }
}
