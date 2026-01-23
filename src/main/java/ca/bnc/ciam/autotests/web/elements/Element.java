package ca.bnc.ciam.autotests.web.elements;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Base element wrapper class that implements IElement interface.
 * Provides enhanced functionality over raw WebElement with null-safety and fluent API.
 *
 * Features:
 * - Null-safe operations (gracefully handles null elements)
 * - Fluent API (method chaining support)
 * - JavaScript fallback operations
 * - Built-in wait operations
 * - Debug helpers (highlight)
 *
 * Usage:
 * <pre>
 * Element element = new Element(driver, webElement, locator);
 * element.scrollIntoView().waitUntilClickable(10).click();
 *
 * if (!element.isNull() && element.isDisplayed()) {
 *     String text = element.getText();
 * }
 * </pre>
 */
@Slf4j
public class Element implements IElement {

    @Getter
    protected final WebDriver driver;

    @Getter
    protected final WebElement baseElement;

    @Getter
    protected final By locator;

    private static final long DEFAULT_TIMEOUT_SECONDS = 10;

    /**
     * Creates an Element wrapper.
     *
     * @param driver  The WebDriver instance
     * @param element The WebElement to wrap (can be null)
     * @param locator The By locator used to find this element
     */
    public Element(WebDriver driver, WebElement element, By locator) {
        this.driver = driver;
        this.baseElement = element;
        this.locator = locator;
    }

    // ==================== State Checking ====================

    @Override
    public boolean isNull() {
        return baseElement == null;
    }

    @Override
    public boolean isDisplayed() {
        if (isNull()) {
            return false;
        }
        try {
            return baseElement.isDisplayed();
        } catch (Exception e) {
            log.debug("Error checking if element is displayed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isEnabled() {
        if (isNull()) {
            return false;
        }
        try {
            return baseElement.isEnabled();
        } catch (Exception e) {
            log.debug("Error checking if element is enabled: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isSelected() {
        if (isNull()) {
            return false;
        }
        try {
            return baseElement.isSelected();
        } catch (Exception e) {
            log.debug("Error checking if element is selected: {}", e.getMessage());
            return false;
        }
    }

    // ==================== Text and Attribute Retrieval ====================

    @Override
    public String getText() {
        if (isNull()) {
            log.debug("getText() called on null element - locator: {}", locator);
            return "";
        }
        try {
            String text = baseElement.getText();
            // If getText() returns empty, try alternative methods
            if (text == null || text.isEmpty()) {
                // Try innerText attribute (works better for some elements)
                text = baseElement.getAttribute("innerText");
            }
            if (text == null || text.isEmpty()) {
                // Try textContent via JavaScript (most reliable fallback)
                text = (String) ((JavascriptExecutor) driver).executeScript(
                    "return arguments[0].textContent || arguments[0].innerText || '';", baseElement);
            }
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            log.debug("Error getting element text: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Gets the visible text of the element using JavaScript.
     * Use this when regular getText() returns empty.
     *
     * @return the text content from JavaScript, or empty string if not found
     */
    public String getTextByJs() {
        if (isNull()) {
            log.debug("getTextByJs() called on null element - locator: {}", locator);
            return "";
        }
        try {
            Object result = ((JavascriptExecutor) driver).executeScript(
                "return arguments[0].textContent || arguments[0].innerText || arguments[0].value || '';",
                baseElement);
            return result != null ? result.toString().trim() : "";
        } catch (Exception e) {
            log.debug("Error getting element text via JS: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public String getAttribute(String attributeName) {
        if (isNull()) {
            return null;
        }
        try {
            return baseElement.getAttribute(attributeName);
        } catch (Exception e) {
            log.debug("Error getting attribute '{}': {}", attributeName, e.getMessage());
            return null;
        }
    }

    @Override
    public String getTagName() {
        if (isNull()) {
            return "";
        }
        try {
            return baseElement.getTagName();
        } catch (Exception e) {
            log.debug("Error getting tag name: {}", e.getMessage());
            return "";
        }
    }

    // ==================== Click Operations ====================

    @Override
    public Element click() {
        if (isNull()) {
            log.warn("Cannot click - element is null");
            return this;
        }
        try {
            baseElement.click();
            log.debug("Clicked element");
        } catch (Exception e) {
            log.error("Error clicking element: {}", e.getMessage());
            throw e;
        }
        return this;
    }

    @Override
    public Element jsClick() {
        if (isNull()) {
            log.warn("Cannot JS click - element is null");
            return this;
        }
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", baseElement);
            log.debug("JS clicked element");
        } catch (Exception e) {
            log.error("Error JS clicking element: {}", e.getMessage());
            throw e;
        }
        return this;
    }

    // ==================== Scroll Operations ====================

    @Override
    public Element scrollIntoView() {
        if (isNull()) {
            log.warn("Cannot scroll - element is null");
            return this;
        }
        try {
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", baseElement);
            log.debug("Scrolled element into view");
        } catch (Exception e) {
            log.error("Error scrolling element into view: {}", e.getMessage());
        }
        return this;
    }

    // ==================== Mouse Operations ====================

    @Override
    public Element hover() {
        if (isNull()) {
            log.warn("Cannot hover - element is null");
            return this;
        }
        try {
            new Actions(driver).moveToElement(baseElement).perform();
            log.debug("Hovered over element");
        } catch (Exception e) {
            log.error("Error hovering over element: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Double-clicks the element.
     *
     * @return this element for chaining
     */
    public Element doubleClick() {
        if (isNull()) {
            log.warn("Cannot double-click - element is null");
            return this;
        }
        try {
            new Actions(driver).doubleClick(baseElement).perform();
            log.debug("Double-clicked element");
        } catch (Exception e) {
            log.error("Error double-clicking element: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Right-clicks (context-clicks) the element.
     *
     * @return this element for chaining
     */
    public Element rightClick() {
        if (isNull()) {
            log.warn("Cannot right-click - element is null");
            return this;
        }
        try {
            new Actions(driver).contextClick(baseElement).perform();
            log.debug("Right-clicked element");
        } catch (Exception e) {
            log.error("Error right-clicking element: {}", e.getMessage());
        }
        return this;
    }

    // ==================== Wait Operations ====================

    @Override
    public Element waitUntilVisible(long timeoutSeconds) {
        if (locator == null) {
            log.warn("Cannot wait - no locator available");
            return this;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
            log.debug("Element is now visible");
        } catch (Exception e) {
            log.warn("Element not visible after {} seconds: {}", timeoutSeconds, e.getMessage());
        }
        return this;
    }

    @Override
    public Element waitUntilClickable(long timeoutSeconds) {
        if (locator == null) {
            log.warn("Cannot wait - no locator available");
            return this;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.elementToBeClickable(locator));
            log.debug("Element is now clickable");
        } catch (Exception e) {
            log.warn("Element not clickable after {} seconds: {}", timeoutSeconds, e.getMessage());
        }
        return this;
    }

    /**
     * Waits for element to be invisible/disappear.
     *
     * @param timeoutSeconds timeout in seconds
     * @return this element for chaining
     */
    public Element waitUntilInvisible(long timeoutSeconds) {
        if (locator == null) {
            log.warn("Cannot wait - no locator available");
            return this;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds))
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            log.debug("Element is now invisible");
        } catch (Exception e) {
            log.warn("Element still visible after {} seconds: {}", timeoutSeconds, e.getMessage());
        }
        return this;
    }

    // ==================== Debug Operations ====================

    @Override
    public Element highlight() {
        if (isNull()) {
            return this;
        }
        try {
            String originalStyle = baseElement.getAttribute("style");
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                baseElement,
                "border: 2px solid red !important; background: yellow !important;"
            );
            Thread.sleep(300);
            ((JavascriptExecutor) driver).executeScript(
                "arguments[0].setAttribute('style', arguments[1]);",
                baseElement,
                originalStyle != null ? originalStyle : ""
            );
        } catch (Exception e) {
            log.debug("Could not highlight element: {}", e.getMessage());
        }
        return this;
    }

    // ==================== Additional Utility Methods ====================

    /**
     * Gets the value attribute (commonly used for input elements).
     *
     * @return the value attribute, or empty string if null
     */
    public String getValue() {
        String value = getAttribute("value");
        return value != null ? value : "";
    }

    /**
     * Gets the CSS value of a property.
     *
     * @param propertyName the CSS property name
     * @return the CSS value, or empty string if null
     */
    public String getCssValue(String propertyName) {
        if (isNull()) {
            return "";
        }
        try {
            return baseElement.getCssValue(propertyName);
        } catch (Exception e) {
            log.debug("Error getting CSS value '{}': {}", propertyName, e.getMessage());
            return "";
        }
    }

    /**
     * Checks if element has a specific CSS class.
     *
     * @param className the class name to check
     * @return true if element has the class
     */
    public boolean hasClass(String className) {
        String classes = getAttribute("class");
        if (classes == null || classes.isEmpty()) {
            return false;
        }
        return (" " + classes + " ").contains(" " + className + " ");
    }

    /**
     * Sets focus on the element.
     *
     * @return this element for chaining
     */
    public Element focus() {
        if (isNull()) {
            log.warn("Cannot focus - element is null");
            return this;
        }
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", baseElement);
            log.debug("Focused element");
        } catch (Exception e) {
            log.debug("Could not focus element: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Blurs (removes focus from) the element.
     *
     * @return this element for chaining
     */
    public Element blur() {
        if (isNull()) {
            log.warn("Cannot blur - element is null");
            return this;
        }
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].blur();", baseElement);
            log.debug("Blurred element");
        } catch (Exception e) {
            log.debug("Could not blur element: {}", e.getMessage());
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("Element[locator=%s, isNull=%s]", locator, isNull());
    }
}
