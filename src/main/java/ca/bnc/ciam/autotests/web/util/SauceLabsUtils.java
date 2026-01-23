package ca.bnc.ciam.autotests.web.util;

import ca.bnc.ciam.autotests.web.WebDriverFactory;
import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Utility class for SauceLabs-specific operations.
 */
@Slf4j
public final class SauceLabsUtils {

    private SauceLabsUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Check if currently running on SauceLabs.
     */
    public static boolean isRunningOnSauceLabs() {
        WebConfig config = WebDriverFactory.getConfig();
        return config != null && config.getExecutionMode() == ExecutionMode.SAUCELABS;
    }

    /**
     * Mark test as passed in SauceLabs.
     */
    public static void markTestPassed(WebDriver driver) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:job-result=passed");
            log.info("Marked SauceLabs test as PASSED");
        }
    }

    /**
     * Mark test as failed in SauceLabs.
     */
    public static void markTestFailed(WebDriver driver) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:job-result=failed");
            log.info("Marked SauceLabs test as FAILED");
        }
    }

    /**
     * Mark test as passed or failed based on boolean.
     */
    public static void markTestResult(WebDriver driver, boolean passed) {
        if (passed) {
            markTestPassed(driver);
        } else {
            markTestFailed(driver);
        }
    }

    /**
     * Set test name in SauceLabs.
     */
    public static void setTestName(WebDriver driver, String name) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:job-name=" + name);
            log.info("Set SauceLabs test name to: {}", name);
        }
    }

    /**
     * Add context/comment to SauceLabs session.
     */
    public static void addContext(WebDriver driver, String context) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:context=" + context);
            log.debug("Added SauceLabs context: {}", context);
        }
    }

    /**
     * Set custom data in SauceLabs.
     */
    public static void setCustomData(WebDriver driver, String key, String value) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, String.format("sauce:custom-data={\"key\":\"%s\",\"value\":\"%s\"}", key, value));
        }
    }

    /**
     * Add tag to SauceLabs session.
     */
    public static void addTag(WebDriver driver, String tag) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:tags=" + tag);
        }
    }

    /**
     * Get SauceLabs session ID.
     */
    public static String getSessionId(WebDriver driver) {
        if (isRemoteDriver(driver)) {
            return ((RemoteWebDriver) driver).getSessionId().toString();
        }
        return null;
    }

    /**
     * Get SauceLabs job link.
     */
    public static String getJobLink(WebDriver driver) {
        String sessionId = getSessionId(driver);
        if (sessionId != null) {
            WebConfig config = WebDriverFactory.getConfig();
            String dataCenter = config != null ? config.getSauceDataCenter() : "us-west-1";
            return String.format("https://app.%s.saucelabs.com/tests/%s", dataCenter, sessionId);
        }
        return null;
    }

    /**
     * Log step in SauceLabs.
     * This creates a named marker in the command log.
     */
    public static void logStep(WebDriver driver, String stepName) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:context=Step: " + stepName);
        }
    }

    /**
     * Enable network capture for SauceLabs.
     */
    public static void enableNetworkCapture(WebDriver driver) {
        if (isRemoteDriver(driver)) {
            executeScript(driver, "sauce:intercept=true");
        }
    }

    /**
     * Stop current session and start a new one with the same configuration.
     */
    public static WebDriver restartSession() {
        WebConfig config = WebDriverFactory.getConfig();
        if (config == null) {
            throw new IllegalStateException("No configuration found for current session");
        }
        WebDriverFactory.quitDriver();
        return WebDriverFactory.createDriver(config);
    }

    /**
     * Check if driver is a RemoteWebDriver (running on SauceLabs or other remote grid).
     */
    private static boolean isRemoteDriver(WebDriver driver) {
        return driver instanceof RemoteWebDriver && isRunningOnSauceLabs();
    }

    /**
     * Execute SauceLabs-specific script.
     */
    private static void executeScript(WebDriver driver, String script) {
        try {
            ((JavascriptExecutor) driver).executeScript(script);
        } catch (Exception e) {
            log.warn("Failed to execute SauceLabs script: {} - {}", script, e.getMessage());
        }
    }
}
