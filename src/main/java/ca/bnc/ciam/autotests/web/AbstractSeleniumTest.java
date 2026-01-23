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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

/**
 * Base class for Selenium-based UI tests.
 * Provides WebDriver lifecycle management and common UI testing utilities.
 */
@Slf4j
public abstract class AbstractSeleniumTest extends AbstractDataDrivenTest {

    protected WebDriver driver;

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
     * Get the base URL for tests. Override in subclass.
     */
    protected abstract String getBaseUrl();

    /**
     * Initialize WebDriver before test class.
     */
    @BeforeClass(alwaysRun = true)
    public void setUpDriver() {
        log.info("Setting up WebDriver for test class: {}", this.getClass().getSimpleName());
        WebConfig config = getWebConfig();

        // Set test/build name for SauceLabs
        if (config.getExecutionMode() == ExecutionMode.SAUCELABS) {
            config = WebConfig.builder()
                    .browserType(config.getBrowserType())
                    .executionMode(config.getExecutionMode())
                    .headless(config.isHeadless())
                    .sauceUsername(config.getSauceUsername())
                    .sauceAccessKey(config.getSauceAccessKey())
                    .testName(this.getClass().getSimpleName())
                    .buildName(System.getProperty("sauce.buildName", "Local Build"))
                    .build();
        }

        driver = WebDriverFactory.createDriver(config);
    }

    /**
     * Navigate to base URL before each test method.
     */
    @BeforeMethod(alwaysRun = true)
    public void navigateToBaseUrl(Method method) {
        log.info("Starting test method: {}", method.getName());
        String baseUrl = getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            driver.get(baseUrl);
            SeleniumUtils.waitForPageLoad(driver);
        }

        // Log step in SauceLabs
        SauceLabsUtils.logStep(driver, method.getName());
    }

    /**
     * Handle test result after each method.
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
