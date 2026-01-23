package ca.bnc.ciam.autotests.web;

import ca.bnc.ciam.autotests.base.AbstractDataDrivenTest;
import ca.bnc.ciam.autotests.web.config.BrowserType;
import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import ca.bnc.ciam.autotests.web.util.SauceLabsUtils;
import ca.bnc.ciam.autotests.web.util.SeleniumUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import java.time.Duration;
import java.util.List;

/**
 * Base class for Selenium-based UI tests.
 * Provides WebDriver lifecycle management and common UI testing utilities.
 *
 * Usage with runApplication():
 * <pre>
 * public class MyTest extends AbstractSeleniumTest {
 *     &#64;Test
 *     public void t000_Start_Application() {
 *         runApplication();  // Uses getBaseUrl()
 *         // or
 *         runApplication("https://custom-url.com");
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class AbstractSeleniumTest extends AbstractDataDrivenTest {

    protected WebDriver driver;
    protected WebConfig webConfig;

    private static final Duration IMPLICIT_WAIT = Duration.ofSeconds(10);
    private static final Duration PAGE_LOAD_TIMEOUT = Duration.ofSeconds(30);

    // ==================== runApplication - Main Entry Point ====================

    /**
     * Run the application - initializes driver and navigates to URL.
     * This is the main entry point for starting tests.
     *
     * URL resolution order:
     * 1. System property "bnc.web.app.url" (pipeline/SauceLabs)
     * 2. getBaseUrl() from subclass
     *
     * Call this from t000_Start_Application() in test classes.
     */
    protected void runApplication() {
        String url = buildURL();
        runApplication(url);
    }

    /**
     * Run the application with a specific URL.
     *
     * @param url The URL to navigate to
     */
    protected void runApplication(String url) {
        log.info("Running application with URL: {}", url);
        initializeDriver();
        driver.get(url);
        SeleniumUtils.waitForPageLoad(driver);
        log.info("Application started at: {}", driver.getCurrentUrl());
    }

    /**
     * Build the application URL.
     * Override in subclass if custom URL logic is needed.
     *
     * URL resolution order:
     * 1. System property "bnc.web.app.url" (pipeline - from context.json)
     * 2. testData "_app_url" (data-driven)
     * 3. System property "web.url" or "webUrl" (local config)
     * 4. getBaseUrl() from subclass (fallback)
     *
     * @return The application URL
     */
    protected String buildURL() {
        // Priority 1: System property bnc.web.app.url (pipeline execution - context.json)
        String appUrl = System.getProperty("bnc.web.app.url");
        if (appUrl != null && !appUrl.isEmpty()) {
            log.info("Using URL from system property bnc.web.app.url (pipeline): {}", appUrl);
            return appUrl;
        }

        // Priority 2: testData (data-driven URL)
        if (testData != null && testData.containsKey("_app_url")) {
            String dataUrl = testData.get("_app_url");
            if (dataUrl != null && !dataUrl.isEmpty()) {
                log.info("Using URL from testData _app_url: {}", dataUrl);
                return dataUrl;
            }
        }

        // Priority 3: System property web.url or webUrl (local config - debug_config.json)
        String webUrl = System.getProperty("web.url");
        if (webUrl == null || webUrl.isEmpty()) {
            webUrl = System.getProperty("webUrl");
        }
        if (webUrl != null && !webUrl.isEmpty()) {
            log.info("Using URL from system property web.url (local): {}", webUrl);
            return webUrl;
        }

        // Priority 4: getBaseUrl() from subclass (fallback)
        String baseUrl = getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            log.info("Using base URL from subclass: {}", baseUrl);
            return baseUrl;
        }

        throw new IllegalStateException(
            "No URL configured. Set bnc.web.app.url (pipeline), web.url (local), " +
            "testData._app_url, or override getBaseUrl()");
    }

    /**
     * Initialize the WebDriver if not already initialized.
     */
    protected void initializeDriver() {
        if (driver == null) {
            log.info("Initializing WebDriver");
            webConfig = getWebConfig();

            // Set test/build name for SauceLabs
            if (webConfig.getExecutionMode() == ExecutionMode.SAUCELABS) {
                webConfig = WebConfig.builder()
                        .browserType(webConfig.getBrowserType())
                        .executionMode(webConfig.getExecutionMode())
                        .headless(webConfig.isHeadless())
                        .sauceUsername(webConfig.getSauceUsername())
                        .sauceAccessKey(webConfig.getSauceAccessKey())
                        .testName(this.getClass().getSimpleName())
                        .buildName(System.getProperty("sauce.buildName", "Local Build"))
                        .build();
            }

            driver = WebDriverFactory.createDriver(webConfig);
            driver.manage().timeouts().implicitlyWait(IMPLICIT_WAIT);
            driver.manage().timeouts().pageLoadTimeout(PAGE_LOAD_TIMEOUT);
            driver.manage().window().maximize();
            log.info("WebDriver initialized - Browser: {}, Headless: {}",
                    webConfig.getBrowserType(), webConfig.isHeadless());
        }
    }

    // ==================== Configuration ====================

    /**
     * Get the WebConfig for this test class.
     * Override to provide custom configuration.
     */
    protected WebConfig getWebConfig() {
        return WebConfig.builder()
                .browserType(getBrowserType())
                .executionMode(getExecutionMode())
                .headless(isHeadless())
                .build();
    }

    /**
     * Get browser type. Override to customize.
     */
    protected BrowserType getBrowserType() {
        return WebDriverFactory.getBrowserFromEnvironment();
    }

    /**
     * Get execution mode. Override to customize.
     */
    protected ExecutionMode getExecutionMode() {
        return WebDriverFactory.getExecutionModeFromEnvironment();
    }

    /**
     * Whether to run headless. Override to customize.
     */
    protected boolean isHeadless() {
        return Boolean.parseBoolean(System.getProperty("headless", "false"));
    }

    /**
     * Get language for testing. Override to customize.
     * Checks testData, system property, or defaults to "en".
     */
    protected String getLanguage() {
        // Priority 1: testData
        if (testData != null && testData.containsKey("_lang")) {
            return testData.get("_lang");
        }
        // Priority 2: System property
        String lang = System.getProperty("bnc.web.gui.lang");
        if (lang != null && !lang.isEmpty()) {
            return lang;
        }
        // Priority 3: Default
        return "en";
    }

    /**
     * Get the base URL for tests.
     * Override in subclass only if you need a hardcoded fallback URL.
     *
     * Normally URL comes from:
     * - Pipeline: bnc.web.app.url system property (from context.json)
     * - Local: web.url system property (from debug_config.json)
     *
     * @return the base URL, or null to require URL from config
     */
    protected String getBaseUrl() {
        return null;  // URL should come from config, not hardcoded
    }

    // ==================== TestNG Lifecycle ====================

    /**
     * Handle test result after each method (for SauceLabs reporting).
     */
    @AfterMethod(alwaysRun = true)
    public void handleTestResult(ITestResult result) {
        String testName = result.getMethod().getMethodName();

        if (result.isSuccess()) {
            log.info("Test method {} PASSED", testName);
        } else {
            log.error("Test method {} FAILED", testName);
            if (result.getThrowable() != null) {
                log.error("Failure reason: {}", result.getThrowable().getMessage());
            }
        }

        // Update SauceLabs result
        SauceLabsUtils.markTestResult(driver, result.isSuccess());
    }

    /**
     * Quit WebDriver after test class.
     */
    @AfterClass(alwaysRun = true)
    public void tearDownDriver() {
        log.info("Tearing down WebDriver for test class: {}", this.getClass().getSimpleName());
        WebDriverFactory.quitDriver();
    }

    // ==================== Convenience Methods ====================

    /**
     * Navigate to a URL.
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
        SeleniumUtils.waitForPageLoad(driver);
    }

    /**
     * Find element by test ID (data-testid attribute).
     */
    protected WebElement findByTestId(String testId) {
        return driver.findElement(By.cssSelector("[data-testid='" + testId + "']"));
    }

    /**
     * Find element by locator.
     */
    protected WebElement findElement(By locator) {
        return driver.findElement(locator);
    }

    /**
     * Find multiple elements.
     */
    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    /**
     * Click element by test ID.
     */
    protected void click(String testId) {
        findByTestId(testId).click();
    }

    /**
     * Click element by locator.
     */
    protected void click(By locator) {
        findElement(locator).click();
    }

    /**
     * Type text into element by test ID.
     */
    protected void type(String testId, String text) {
        WebElement element = findByTestId(testId);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Type text into element by locator.
     */
    protected void type(By locator, String text) {
        WebElement element = findElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Get text from element.
     */
    protected String getText(By locator) {
        return findElement(locator).getText();
    }

    /**
     * Get text from element by test ID.
     */
    protected String getText(String testId) {
        return findByTestId(testId).getText();
    }

    /**
     * Check if element is displayed.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Wait for element to be visible.
     */
    protected void waitForVisible(By locator, Duration timeout) {
        SeleniumUtils.waitFor(driver,
                org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(locator),
                timeout);
    }

    /**
     * Wait for element to be visible with default timeout.
     */
    protected void waitForVisible(By locator) {
        waitForVisible(locator, Duration.ofSeconds(10));
    }

    /**
     * Take screenshot.
     */
    protected byte[] takeScreenshot() {
        return SeleniumUtils.takeScreenshotAsBytes(driver);
    }

    /**
     * Get current URL.
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Get page title.
     */
    protected String getTitle() {
        return driver.getTitle();
    }
}
