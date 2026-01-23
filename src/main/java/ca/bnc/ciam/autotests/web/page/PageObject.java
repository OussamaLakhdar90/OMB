package ca.bnc.ciam.autotests.web.page;

import ca.bnc.ciam.autotests.base.AbstractDataDrivenTest;
import ca.bnc.ciam.autotests.web.WebDriverFactory;
import ca.bnc.ciam.autotests.web.resources.ResourcesClass;
import ca.bnc.ciam.autotests.web.util.SeleniumUtils;
import lombok.Getter;
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
 *
 * Supports internationalization through resource bundles:
 * - Use getResource(resourceName) to load page-specific properties
 * - Language is auto-detected from browser or can be set explicitly
 *
 * Example:
 * <pre>
 * public class LoginPage extends PageObject {
 *     private final ResourcesClass resource;
 *
 *     public LoginPage(WebDriver driver) {
 *         super(driver);
 *         this.resource = getResource("login_page");
 *     }
 *
 *     public String getUsernameLabel() {
 *         return resource.get("username.label");
 *     }
 * }
 * </pre>
 */
@Slf4j
public abstract class PageObject extends AbstractDataDrivenTest {

    protected static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    protected static final Duration SHORT_TIMEOUT = Duration.ofSeconds(5);
    protected static final Duration LONG_TIMEOUT = Duration.ofSeconds(30);
    protected static final String DEFAULT_LANGUAGE = "en";

    protected WebDriver driver;
    protected WebDriverWait wait;

    @Getter
    private String language;

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

    // ==================== Element Builders ====================

    /**
     * Builds an Element wrapper for the given locator with default timeout.
     *
     * @param locator the By locator
     * @return Element wrapper (may contain null WebElement if not found)
     */
    protected ca.bnc.ciam.autotests.web.elements.Element buildElement(By locator) {
        return buildElement(DEFAULT_TIMEOUT.toSeconds(), locator);
    }

    /**
     * Builds an Element wrapper with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @param locator        the By locator
     * @return Element wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.Element buildElement(long timeoutSeconds, By locator) {
        WebElement element = findElementSafely(locator, timeoutSeconds);
        return new ca.bnc.ciam.autotests.web.elements.Element(driver, element, locator);
    }

    /**
     * Builds a TextField wrapper for the given locator.
     *
     * @param locator the By locator
     * @return TextField wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.TextField buildTextField(By locator) {
        return buildTextField(DEFAULT_TIMEOUT.toSeconds(), locator);
    }

    /**
     * Builds a TextField wrapper with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @param locator        the By locator
     * @return TextField wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.TextField buildTextField(long timeoutSeconds, By locator) {
        WebElement element = findElementSafely(locator, timeoutSeconds);
        return new ca.bnc.ciam.autotests.web.elements.TextField(driver, element, locator);
    }

    /**
     * Builds a Button wrapper for the given locator.
     *
     * @param locator the By locator
     * @return Button wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.Button buildButton(By locator) {
        return buildButton(DEFAULT_TIMEOUT.toSeconds(), locator);
    }

    /**
     * Builds a Button wrapper with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @param locator        the By locator
     * @return Button wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.Button buildButton(long timeoutSeconds, By locator) {
        WebElement element = findElementSafely(locator, timeoutSeconds);
        return new ca.bnc.ciam.autotests.web.elements.Button(driver, element, locator);
    }

    /**
     * Builds a CheckBox wrapper for the given locator.
     *
     * @param locator the By locator
     * @return CheckBox wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.CheckBox buildCheckBox(By locator) {
        return buildCheckBox(DEFAULT_TIMEOUT.toSeconds(), locator);
    }

    /**
     * Builds a CheckBox wrapper with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @param locator        the By locator
     * @return CheckBox wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.CheckBox buildCheckBox(long timeoutSeconds, By locator) {
        WebElement element = findElementSafely(locator, timeoutSeconds);
        return new ca.bnc.ciam.autotests.web.elements.CheckBox(driver, element, locator);
    }

    /**
     * Builds an Image wrapper for the given locator.
     *
     * @param locator the By locator
     * @return Image wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.Image buildImage(By locator) {
        return buildImage(DEFAULT_TIMEOUT.toSeconds(), locator);
    }

    /**
     * Builds an Image wrapper with custom timeout.
     *
     * @param timeoutSeconds timeout in seconds
     * @param locator        the By locator
     * @return Image wrapper
     */
    protected ca.bnc.ciam.autotests.web.elements.Image buildImage(long timeoutSeconds, By locator) {
        WebElement element = findElementSafely(locator, timeoutSeconds);
        return new ca.bnc.ciam.autotests.web.elements.Image(driver, element, locator);
    }

    /**
     * Finds element safely without throwing exception.
     *
     * @param locator        the By locator
     * @param timeoutSeconds timeout in seconds
     * @return WebElement or null if not found
     */
    private WebElement findElementSafely(By locator, long timeoutSeconds) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            log.debug("Element not found: {} (timeout: {}s)", locator, timeoutSeconds);
            return null;
        }
    }

    // ==================== Resource/i18n Methods ====================

    /**
     * Gets a ResourcesClass for the specified resource file name.
     * Uses the browser language or the configured language.
     *
     * Resource files should be named: {resourceName}_{language}.properties
     * e.g., login_page_en.properties, login_page_fr.properties
     *
     * @param resourceName the base name of the resource file (without language suffix)
     * @return ResourcesClass instance for accessing localized strings
     */
    protected ResourcesClass getResource(String resourceName) {
        String lang = getLanguage();
        if (lang == null || lang.isEmpty()) {
            lang = detectBrowserLanguage();
            this.language = lang;
        }
        return new ResourcesClass(resourceName, lang, this);
    }

    /**
     * Gets a ResourcesClass for the specified resource file name with a specific language.
     *
     * @param resourceName the base name of the resource file
     * @param language     the language code (e.g., "en", "fr")
     * @return ResourcesClass instance
     */
    protected ResourcesClass getResource(String resourceName, String language) {
        return new ResourcesClass(resourceName, language, this);
    }

    /**
     * Sets the language for resource loading.
     * This overrides the auto-detected browser language.
     *
     * @param language the language code (e.g., "en", "fr")
     */
    public void setLanguage(String language) {
        this.language = language;
        log.debug("Language set to: {}", language);
    }

    /**
     * Detects the browser's language from navigator.language.
     * Falls back to DEFAULT_LANGUAGE if detection fails.
     *
     * @return the detected language code (2-letter code like "en", "fr")
     */
    protected String detectBrowserLanguage() {
        try {
            Object result = executeScript("return navigator.language || navigator.userLanguage;");
            if (result != null) {
                String browserLang = result.toString();
                // Extract 2-letter code (e.g., "en-US" -> "en", "fr-CA" -> "fr")
                if (browserLang.contains("-")) {
                    browserLang = browserLang.split("-")[0];
                }
                if (browserLang.contains("_")) {
                    browserLang = browserLang.split("_")[0];
                }
                log.debug("Detected browser language: {}", browserLang);
                return browserLang.toLowerCase();
            }
        } catch (Exception e) {
            log.warn("Could not detect browser language: {}", e.getMessage());
        }
        log.debug("Using default language: {}", DEFAULT_LANGUAGE);
        return DEFAULT_LANGUAGE;
    }

    /**
     * Gets the current language being used for resources.
     * If not set, detects from browser.
     *
     * @return the current language code
     */
    public String getCurrentLanguage() {
        if (language == null || language.isEmpty()) {
            language = detectBrowserLanguage();
        }
        return language;
    }
}
