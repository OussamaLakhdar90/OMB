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

    // ==================== Pipeline/Hub Configuration ====================

    /**
     * Whether to use remote hub (SauceLabs tunnel).
     * Maps to context.json: bnc.test.hub.use
     */
    @Builder.Default
    private boolean useHub = false;

    /**
     * Remote hub URL (SauceLabs tunnel URL).
     * Maps to context.json: bnc.test.hub.url
     */
    private String hubUrl;

    /**
     * Tunnel name for SauceLabs.
     * Maps to context.json: bnc.test.hub.name or sauce:options.tunnelIdentifier
     */
    private String tunnelName;

    /**
     * Tunnel owner (parent tunnel) for shared tunnels.
     * Maps to sauce:options.parentTunnel
     */
    private String tunnelOwner;

    /**
     * Path to browser configuration file.
     * Maps to context.json: bnc.web.browsers.config
     */
    private String browserConfigPath;

    /**
     * Extended debugging for SauceLabs.
     */
    @Builder.Default
    private boolean extendedDebugging = true;

    /**
     * Screen resolution for SauceLabs.
     */
    @Builder.Default
    private String screenResolution = "1920x1080";

    /**
     * Idle timeout for SauceLabs (in seconds).
     */
    @Builder.Default
    private int idleTimeout = 300;

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
     * Create a pipeline configuration using hub URL.
     *
     * @param hubUrl     The SauceLabs tunnel URL
     * @param tunnelName The tunnel name (tunnelIdentifier)
     * @param tunnelOwner The tunnel owner (parentTunnel)
     */
    public static WebConfig pipeline(String hubUrl, String tunnelName, String tunnelOwner) {
        return WebConfig.builder()
                .executionMode(ExecutionMode.SAUCELABS)
                .useHub(true)
                .hubUrl(hubUrl)
                .tunnelName(tunnelName)
                .tunnelOwner(tunnelOwner)
                .build();
    }

    /**
     * Get the SauceLabs hub URL.
     * Priority: 1) Custom hubUrl (pipeline tunnel), 2) Standard SauceLabs URL
     */
    public String getSauceLabsUrl() {
        // If custom hub URL is set (pipeline mode), use it
        if (hubUrl != null && !hubUrl.isEmpty()) {
            return hubUrl;
        }
        // Otherwise use standard SauceLabs URL
        return String.format("https://%s:%s@ondemand.%s.saucelabs.com:443/wd/hub",
                sauceUsername, sauceAccessKey, sauceDataCenter);
    }

    /**
     * Check if running in pipeline mode (using hub URL).
     */
    public boolean isPipelineMode() {
        return useHub && hubUrl != null && !hubUrl.isEmpty();
    }
}
