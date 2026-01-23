package ca.bnc.ciam.autotests.web.config;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Configuration for WebDriver instances.
 */
@Data
@Builder
public class WebConfig {

    /**
     * Browser type to use.
     */
    @Builder.Default
    private BrowserType browserType = BrowserType.CHROME;

    /**
     * Execution mode (local or SauceLabs).
     */
    @Builder.Default
    private ExecutionMode executionMode = ExecutionMode.LOCAL;

    /**
     * Whether to run browser in headless mode.
     */
    @Builder.Default
    private boolean headless = false;

    /**
     * Implicit wait timeout.
     */
    @Builder.Default
    private Duration implicitWait = Duration.ofSeconds(10);

    /**
     * Page load timeout.
     */
    @Builder.Default
    private Duration pageLoadTimeout = Duration.ofSeconds(30);

    /**
     * Script timeout.
     */
    @Builder.Default
    private Duration scriptTimeout = Duration.ofSeconds(30);

    /**
     * Browser window width.
     */
    @Builder.Default
    private int windowWidth = 1920;

    /**
     * Browser window height.
     */
    @Builder.Default
    private int windowHeight = 1080;

    /**
     * Whether to maximize window on start.
     */
    @Builder.Default
    private boolean maximizeWindow = true;

    /**
     * SauceLabs username (for remote execution).
     */
    private String sauceUsername;

    /**
     * SauceLabs access key (for remote execution).
     */
    private String sauceAccessKey;

    /**
     * SauceLabs data center (us-west-1, eu-central-1, etc.).
     */
    @Builder.Default
    private String sauceDataCenter = "us-west-1";

    /**
     * Platform/OS for SauceLabs.
     */
    @Builder.Default
    private String platform = "Windows 11";

    /**
     * Browser version for SauceLabs.
     */
    @Builder.Default
    private String browserVersion = "latest";

    /**
     * Test name for SauceLabs reporting.
     */
    private String testName;

    /**
     * Build name for SauceLabs reporting.
     */
    private String buildName;

    /**
     * Path to browser binary (optional, for custom browser installations).
     */
    private String browserBinaryPath;

    /**
     * Path to WebDriver executable (optional, for manual driver management).
     */
    private String driverPath;

    /**
     * Whether to enable browser developer tools/logging.
     */
    @Builder.Default
    private boolean enableDevTools = false;

    /**
     * Whether to accept insecure certificates.
     */
    @Builder.Default
    private boolean acceptInsecureCerts = true;

    /**
     * Create a default local Chrome configuration.
     */
    public static WebConfig defaultConfig() {
        return WebConfig.builder().build();
    }

    /**
     * Create a headless Chrome configuration.
     */
    public static WebConfig headlessChrome() {
        return WebConfig.builder()
                .browserType(BrowserType.CHROME)
                .headless(true)
                .build();
    }

    /**
     * Create a SauceLabs configuration.
     */
    public static WebConfig sauceLabs(String username, String accessKey, BrowserType browser) {
        return WebConfig.builder()
                .executionMode(ExecutionMode.SAUCELABS)
                .browserType(browser)
                .sauceUsername(username)
                .sauceAccessKey(accessKey)
                .build();
    }

    /**
     * Get the SauceLabs hub URL.
     */
    public String getSauceLabsUrl() {
        return String.format("https://%s:%s@ondemand.%s.saucelabs.com:443/wd/hub",
                sauceUsername, sauceAccessKey, sauceDataCenter);
    }
}
