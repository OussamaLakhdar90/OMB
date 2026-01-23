package ca.bnc.ciam.autotests.web.page;

import ca.bnc.ciam.autotests.base.AbstractDataDrivenTest;
import ca.bnc.ciam.autotests.web.WebDriverFactory;
import ca.bnc.ciam.autotests.web.util.SeleniumUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Base class for Page Objects.
 * Provides common methods for interacting with web pages.
 */
@Slf4j
public abstract class PageObject extends AbstractDataDrivenTest {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    protected static final Duration SHORT_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);

    protected WebDriver driver;
    protected WebDriverWait wait;

    /**
     * Initialize PageObject with the current thread's WebDriver.
     */
    protected PageObject() {
        this.driver = WebDriverFactory.getDriver();
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        PageFactory.initElements(driver, this);
    }

    /**
     * Initialize PageObject with a specific WebDriver.
     */
    protected PageObject(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        PageFactory.initElements(driver, this);
    }

    /**
     * Initialize PageObject with a specific timeout.
     */
    protected PageObject(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
        PageFactory.initElements(driver, this);
    }

    // ==================== Navigation ====================

    /**
     * Navigate to a URL.
     */
    protected void navigateTo(String url) {
        log.info("Navigating to: {}", url);
        driver.get(url);
    }

    /**
     * Get the current URL.
     */
    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    /**
     * Get the page title.
     */
    protected String getPageTitle() {
        return driver.getTitle();
    }

    /**
     * Refresh the current page.
     */
    protected void refresh() {
        log.info("Refreshing page");
        driver.navigate().refresh();
    }

    /**
     * Navigate back.
     */
    protected void navigateBack() {
        log.info("Navigating back");
        driver.navigate().back();
    }

    /**
     * Navigate forward.
     */
    protected void navigateForward() {
        log.info("Navigating forward");
        driver.navigate().forward();
    }

    // ==================== Element Finding ====================

    /**
     * Find element by CSS selector with data-testid attribute.
     * This is the preferred locator strategy.
     */
    protected WebElement findByTestId(String testId) {
        return findElement(By.cssSelector("[data-testid='" + testId + "']"));
    }

    /**
     * Find element with wait.
     */
    protected WebElement findElement(By locator) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Find element with custom timeout.
     */
    protected WebElement findElement(By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Find multiple elements.
     */
    protected List<WebElement> findElements(By locator) {
        return driver.findElements(locator);
    }

    /**
     * Find visible element.
     */
    protected WebElement findVisibleElement(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Find clickable element.
     */
    protected WebElement findClickableElement(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    // ==================== Element Interactions ====================

    /**
     * Click an element.
     */
    protected void click(By locator) {
        log.debug("Clicking element: {}", locator);
        findClickableElement(locator).click();
    }

    /**
     * Click an element by test ID.
     */
    protected void clickByTestId(String testId) {
        click(By.cssSelector("[data-testid='" + testId + "']"));
    }

    /**
     * Click using JavaScript (for elements that are difficult to click normally).
     */
    protected void jsClick(By locator) {
        log.debug("JS clicking element: {}", locator);
        WebElement element = findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    /**
     * Type text into an input field.
     */
    protected void type(By locator, String text) {
        log.debug("Typing '{}' into element: {}", text, locator);
        WebElement element = findVisibleElement(locator);
        element.clear();
        element.sendKeys(text);
    }

    /**
     * Type text by test ID.
     */
    protected void typeByTestId(String testId, String text) {
        type(By.cssSelector("[data-testid='" + testId + "']"), text);
    }

    /**
     * Clear an input field.
     */
    protected void clear(By locator) {
        log.debug("Clearing element: {}", locator);
        findVisibleElement(locator).clear();
    }

    /**
     * Get text from an element.
     */
    protected String getText(By locator) {
        return findVisibleElement(locator).getText();
    }

    /**
     * Get text by test ID.
     */
    protected String getTextByTestId(String testId) {
        return getText(By.cssSelector("[data-testid='" + testId + "']"));
    }

    /**
     * Get attribute value from an element.
     */
    protected String getAttribute(By locator, String attribute) {
        return findElement(locator).getAttribute(attribute);
    }

    /**
     * Get value from input element.
     */
    protected String getValue(By locator) {
        return getAttribute(locator, "value");
    }

    // ==================== Element State ====================

    /**
     * Check if element is displayed.
     */
    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is enabled.
     */
    protected boolean isEnabled(By locator) {
        try {
            return findElement(locator).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is selected (for checkboxes/radio buttons).
     */
    protected boolean isSelected(By locator) {
        try {
            return findElement(locator).isSelected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element exists in DOM.
     */
    protected boolean exists(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    // ==================== Waiting ====================

    /**
     * Wait for element to be visible.
     */
    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Wait for element to be invisible.
     */
    protected boolean waitForInvisible(By locator) {
        return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Wait for element to be clickable.
     */
    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait for text to be present in element.
     */
    protected boolean waitForTextPresent(By locator, String text) {
        return wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Wait for URL to contain text.
     */
    protected boolean waitForUrlContains(String text) {
        return wait.until(ExpectedConditions.urlContains(text));
    }

    /**
     * Wait for page title to contain text.
     */
    protected boolean waitForTitleContains(String text) {
        return wait.until(ExpectedConditions.titleContains(text));
    }

    /**
     * Wait for a specific condition using SeleniumUtils.
     */
    protected void waitFor(Duration duration) {
        SeleniumUtils.sleep(duration);
    }

    // ==================== JavaScript Execution ====================

    /**
     * Execute JavaScript.
     */
    protected Object executeScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /**
     * Execute async JavaScript.
     */
    protected Object executeAsyncScript(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeAsyncScript(script, args);
    }

    /**
     * Scroll element into view.
     */
    protected void scrollIntoView(By locator) {
        WebElement element = findElement(locator);
        executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
    }

    /**
     * Scroll to top of page.
     */
    protected void scrollToTop() {
        executeScript("window.scrollTo(0, 0);");
    }

    /**
     * Scroll to bottom of page.
     */
    protected void scrollToBottom() {
        executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }

    // ==================== Frames and Windows ====================

    /**
     * Switch to frame by locator.
     */
    protected void switchToFrame(By locator) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    /**
     * Switch to frame by index.
     */
    protected void switchToFrame(int index) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(index));
    }

    /**
     * Switch to default content (exit frame).
     */
    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    /**
     * Switch to new window/tab.
     */
    protected void switchToNewWindow() {
        String originalHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(originalHandle)) {
                driver.switchTo().window(handle);
                return;
            }
        }
    }

    /**
     * Switch to window by handle.
     */
    protected void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    /**
     * Get current window handle.
     */
    protected String getWindowHandle() {
        return driver.getWindowHandle();
    }

    // ==================== Alerts ====================

    /**
     * Accept alert.
     */
    protected void acceptAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    /**
     * Dismiss alert.
     */
    protected void dismissAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).dismiss();
    }

    /**
     * Get alert text.
     */
    protected String getAlertText() {
        return wait.until(ExpectedConditions.alertIsPresent()).getText();
    }

    /**
     * Send keys to alert.
     */
    protected void typeInAlert(String text) {
        wait.until(ExpectedConditions.alertIsPresent()).sendKeys(text);
    }

    // ==================== Page Load ====================

    /**
     * Wait for page to be fully loaded.
     */
    protected void waitForPageLoad() {
        wait.until(driver -> executeScript("return document.readyState").equals("complete"));
    }

    /**
     * Wait for jQuery/AJAX calls to complete.
     */
    protected void waitForAjax() {
        wait.until(driver -> {
            try {
                return (Boolean) executeScript("return jQuery.active == 0");
            } catch (Exception e) {
                return true; // jQuery not present
            }
        });
    }

    /**
     * Get the WebDriver instance.
     */
    public WebDriver getDriver() {
        return driver;
    }
}
