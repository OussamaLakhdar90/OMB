package ca.bnc.ciam.autotests.web;

import ca.bnc.ciam.autotests.web.builder.ChromeDriverBuilder;
import ca.bnc.ciam.autotests.web.builder.EdgeDriverBuilder;
import ca.bnc.ciam.autotests.web.builder.FirefoxDriverBuilder;
import ca.bnc.ciam.autotests.web.builder.IEDriverBuilder;
import ca.bnc.ciam.autotests.web.builder.IWebDriverBuilder;
import ca.bnc.ciam.autotests.web.builder.SafariDriverBuilder;
import ca.bnc.ciam.autotests.web.config.BrowserType;
import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for creating WebDriver instances.
 * Manages driver lifecycle with ThreadLocal storage for thread safety.
 */
@Slf4j
public class WebDriverFactory {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<WebConfig> configThreadLocal = new ThreadLocal<>();

    private static final Map<BrowserType, IWebDriverBuilder> builders = new EnumMap<>(BrowserType.class);

    static {
        // Register all browser builders
        builders.put(BrowserType.CHROME, new ChromeDriverBuilder());
        builders.put(BrowserType.FIREFOX, new FirefoxDriverBuilder());
        builders.put(BrowserType.EDGE, new EdgeDriverBuilder());
        builders.put(BrowserType.SAFARI, new SafariDriverBuilder());
        builders.put(BrowserType.IE, new IEDriverBuilder());
    }

    private WebDriverFactory() {
        // Utility class - prevent instantiation
    }

    /**
     * Create a WebDriver with default Chrome configuration.
     */
    public static WebDriver createDriver() {
        return createDriver(WebConfig.defaultConfig());
    }

    /**
     * Create a WebDriver with specified browser type.
     */
    public static WebDriver createDriver(BrowserType browserType) {
        return createDriver(WebConfig.builder().browserType(browserType).build());
    }

    /**
     * Create a WebDriver with specified browser type and headless mode.
     */
    public static WebDriver createDriver(BrowserType browserType, boolean headless) {
        return createDriver(WebConfig.builder()
                .browserType(browserType)
                .headless(headless)
                .build());
    }

    /**
     * Create a WebDriver with full configuration.
     */
    public static WebDriver createDriver(WebConfig config) {
        log.info("Creating WebDriver: browser={}, mode={}, headless={}",
                config.getBrowserType(), config.getExecutionMode(), config.isHeadless());

        IWebDriverBuilder builder = builders.get(config.getBrowserType());
        if (builder == null) {
            throw new IllegalArgumentException("No builder registered for browser: " + config.getBrowserType());
        }

        WebDriver driver = builder.build(config);
        driverThreadLocal.set(driver);
        configThreadLocal.set(config);

        log.info("WebDriver created successfully");
        return driver;
    }

    /**
     * Create a WebDriver for SauceLabs.
     */
    public static WebDriver createSauceLabsDriver(BrowserType browserType,
                                                   String username,
                                                   String accessKey,
                                                   String testName) {
        WebConfig config = WebConfig.builder()
                .browserType(browserType)
                .executionMode(ExecutionMode.SAUCELABS)
                .sauceUsername(username)
                .sauceAccessKey(accessKey)
                .testName(testName)
                .build();

        return createDriver(config);
    }

    /**
     * Get the current thread's WebDriver instance.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException("No WebDriver has been created for this thread. Call createDriver() first.");
        }
        return driver;
    }

    /**
     * Get the current thread's WebDriver instance or null if not created.
     */
    public static WebDriver getDriverOrNull() {
        return driverThreadLocal.get();
    }

    /**
     * Get the current thread's WebConfig.
     */
    public static WebConfig getConfig() {
        return configThreadLocal.get();
    }

    /**
     * Check if a WebDriver exists for the current thread.
     */
    public static boolean hasDriver() {
        return driverThreadLocal.get() != null;
    }

    /**
     * Quit the current thread's WebDriver and clean up resources.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                log.info("Quitting WebDriver");
                driver.quit();
            } catch (Exception e) {
                log.warn("Error while quitting WebDriver", e);
            } finally {
                driverThreadLocal.remove();
                configThreadLocal.remove();
            }
        }
    }

    /**
     * Close the current window without quitting the driver.
     */
    public static void closeWindow() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.close();
            } catch (Exception e) {
                log.warn("Error while closing window", e);
            }
        }
    }

    /**
     * Register a custom browser builder.
     */
    public static void registerBuilder(BrowserType browserType, IWebDriverBuilder builder) {
        builders.put(browserType, builder);
        log.info("Registered custom builder for browser: {}", browserType);
    }

    /**
     * Get environment-based browser type.
     * Reads from system property 'browser' or environment variable 'BROWSER'.
     */
    public static BrowserType getBrowserFromEnvironment() {
        String browser = System.getProperty("browser");
        if (browser == null || browser.isEmpty()) {
            browser = System.getenv("BROWSER");
        }
        if (browser == null || browser.isEmpty()) {
            return BrowserType.CHROME; // Default
        }
        return BrowserType.fromString(browser);
    }

    /**
     * Get environment-based execution mode.
     * Reads from system property 'execution.mode' or environment variable 'EXECUTION_MODE'.
     */
    public static ExecutionMode getExecutionModeFromEnvironment() {
        String mode = System.getProperty("execution.mode");
        if (mode == null || mode.isEmpty()) {
            mode = System.getenv("EXECUTION_MODE");
        }
        if (mode == null || mode.isEmpty()) {
            return ExecutionMode.LOCAL; // Default
        }
        return ExecutionMode.fromString(mode);
    }

    /**
     * Create a WebDriver based on environment variables/system properties.
     */
    public static WebDriver createDriverFromEnvironment() {
        BrowserType browserType = getBrowserFromEnvironment();
        ExecutionMode executionMode = getExecutionModeFromEnvironment();

        WebConfig.WebConfigBuilder configBuilder = WebConfig.builder()
                .browserType(browserType)
                .executionMode(executionMode)
                .headless(Boolean.parseBoolean(System.getProperty("headless", "false")));

        if (executionMode == ExecutionMode.SAUCELABS) {
            configBuilder
                    .sauceUsername(System.getProperty("sauce.username", System.getenv("SAUCE_USERNAME")))
                    .sauceAccessKey(System.getProperty("sauce.accessKey", System.getenv("SAUCE_ACCESS_KEY")))
                    .testName(System.getProperty("sauce.testName", "Automated Test"))
                    .buildName(System.getProperty("sauce.buildName", "Local Build"));
        }

        return createDriver(configBuilder.build());
    }

    /**
     * Create a WebDriver for pipeline execution.
     * Uses context.json properties: bnc.test.hub.use, bnc.test.hub.url, bnc.test.hub.name, bnc.web.browsers.config
     *
     * @param testName Test name for reporting
     * @return WebDriver configured for pipeline
     */
    public static WebDriver createDriverForPipeline(String testName) {
        boolean useHub = Boolean.parseBoolean(System.getProperty("bnc.test.hub.use", "false"));
        String hubUrl = System.getProperty("bnc.test.hub.url");
        String tunnelName = System.getProperty("bnc.test.hub.name");
        String browserConfigPath = System.getProperty("bnc.web.browsers.config");

        log.info("Creating pipeline driver: useHub={}, hubUrl={}, tunnelName={}, browserConfig={}",
                useHub, hubUrl != null ? "***" : null, tunnelName, browserConfigPath);

        if (useHub && hubUrl != null && !hubUrl.isEmpty()) {
            // SauceLabs execution via tunnel
            return createSauceLabsDriverFromPipeline(hubUrl, tunnelName, browserConfigPath, testName);
        } else {
            // Local execution on pipeline agent
            return createLocalPipelineDriver(browserConfigPath, testName);
        }
    }

    /**
     * Create a SauceLabs driver for pipeline execution.
     */
    private static WebDriver createSauceLabsDriverFromPipeline(String hubUrl, String tunnelName,
                                                                String browserConfigPath, String testName) {
        // Get browser type from config or default
        BrowserType browserType = getBrowserFromConfig(browserConfigPath);

        // Get sauce credentials
        String sauceUsername = System.getProperty("sauce.username", System.getenv("SAUCE_USERNAME"));
        String sauceAccessKey = System.getProperty("sauce.accessKey", System.getenv("SAUCE_ACCESS_KEY"));

        // Get tunnel owner (parentTunnel) - loaded from browser config or system property
        String tunnelOwner = System.getProperty("bnc.test.hub.owner");

        WebConfig config = WebConfig.builder()
                .browserType(browserType)
                .executionMode(ExecutionMode.SAUCELABS)
                .useHub(true)
                .hubUrl(hubUrl)
                .tunnelName(tunnelName)
                .tunnelOwner(tunnelOwner)
                .browserConfigPath(browserConfigPath)
                .sauceUsername(sauceUsername)
                .sauceAccessKey(sauceAccessKey)
                .testName(testName)
                .buildName(System.getProperty("sauce.buildName", "Pipeline Build"))
                .build();

        return createDriver(config);
    }

    /**
     * Create a local driver for pipeline agent execution.
     */
    private static WebDriver createLocalPipelineDriver(String browserConfigPath, String testName) {
        BrowserType browserType = getBrowserFromConfig(browserConfigPath);

        WebConfig config = WebConfig.builder()
                .browserType(browserType)
                .executionMode(ExecutionMode.LOCAL)
                .browserConfigPath(browserConfigPath)
                .headless(Boolean.parseBoolean(System.getProperty("headless", "true"))) // Default headless for pipeline
                .build();

        return createDriver(config);
    }

    /**
     * Get browser type from browser config file.
     */
    private static BrowserType getBrowserFromConfig(String browserConfigPath) {
        if (browserConfigPath != null && !browserConfigPath.isEmpty()) {
            try {
                var browserConfig = ca.bnc.ciam.autotests.web.config.BrowserConfigLoader.getInstance()
                        .loadPipelineConfig(browserConfigPath);
                String browserName = browserConfig.getBrowserName();
                if (browserName != null && !browserName.isEmpty()) {
                    return BrowserType.fromString(browserName);
                }
            } catch (Exception e) {
                log.warn("Failed to get browser from config {}: {}", browserConfigPath, e.getMessage());
            }
        }
        return getBrowserFromEnvironment();
    }

    /**
     * Check if pipeline mode is enabled.
     * Pipeline mode uses bnc.test.hub.use property.
     */
    public static boolean isPipelineMode() {
        return Boolean.parseBoolean(System.getProperty("bnc.test.hub.use", "false"));
    }

    /**
     * Check if using SauceLabs hub.
     */
    public static boolean isUsingSauceLabsHub() {
        return isPipelineMode() && System.getProperty("bnc.test.hub.url") != null;
    }
}
