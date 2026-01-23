package ca.bnc.ciam.autotests.web.elements;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * TextField wrapper class for text input elements.
 * Extends Element with text input specific methods.
 *
 * Features:
 * - Send keys with clear option
 * - Type slowly (simulates human typing)
 * - Clear operations (standard and keyboard-based)
 * - Value retrieval
 * - Input state checking (readonly, maxlength, placeholder)
 * - Keyboard key presses (Enter, Tab, Escape)
 *
 * Usage:
 * <pre>
 * TextField username = new TextField(driver, webElement, locator);
 * username.clearAndType("john.doe");
 * username.pressEnter();
 * </pre>
 */
@Slf4j
public class TextField extends Element {

    /**
     * Creates a TextField wrapper.
     *
     * @param driver  The WebDriver instance
     * @param element The WebElement to wrap (can be null)
     * @param locator The By locator used to find this element
     */
    public TextField(WebDriver driver, WebElement element, By locator) {
        super(driver, element, locator);
    }

    // ==================== Input Operations ====================

    /**
     * Types text into the field (appends to existing content).
     *
     * @param text the text to type
     * @return this element for chaining
     */
    public TextField sendKeys(String text) {
        if (isNull()) {
            log.warn("Cannot send keys - element is null");
            return this;
        }
        if (text == null) {
            log.warn("Cannot send null text");
            return this;
        }
        try {
            baseElement.sendKeys(text);
            log.debug("Sent keys to element");
        } catch (Exception e) {
            log.error("Error sending keys: {}", e.getMessage());
            throw e;
        }
        return this;
    }

    /**
     * Types text slowly, character by character (simulates human typing).
     *
     * @param text       the text to type
     * @param delayMillis delay between each character in milliseconds
     * @return this element for chaining
     */
    public TextField typeSlowly(String text, long delayMillis) {
        if (isNull()) {
            log.warn("Cannot type slowly - element is null");
            return this;
        }
        if (text == null) {
            return this;
        }
        try {
            for (char c : text.toCharArray()) {
                baseElement.sendKeys(String.valueOf(c));
                Thread.sleep(delayMillis);
            }
            log.debug("Typed slowly into element");
        } catch (Exception e) {
            log.error("Error typing slowly: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Clears the field content.
     *
     * @return this element for chaining
     */
    public TextField clear() {
        if (isNull()) {
            log.warn("Cannot clear - element is null");
            return this;
        }
        try {
            baseElement.clear();
            log.debug("Cleared element");
        } catch (Exception e) {
            log.error("Error clearing element: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Clears field using keyboard (Ctrl+A, Delete) - useful when clear() doesn't work.
     *
     * @return this element for chaining
     */
    public TextField clearWithKeyboard() {
        if (isNull()) {
            log.warn("Cannot clear with keyboard - element is null");
            return this;
        }
        try {
            baseElement.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            log.debug("Cleared element with keyboard");
        } catch (Exception e) {
            log.error("Error clearing with keyboard: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Clears the field and types new text.
     *
     * @param text the text to type
     * @return this element for chaining
     */
    public TextField clearAndType(String text) {
        clear();
        return sendKeys(text);
    }

    // ==================== Value Retrieval ====================

    /**
     * Gets the current value of the text field.
     *
     * @return the value attribute content, or empty string if null
     */
    @Override
    public String getValue() {
        return super.getValue();
    }

    /**
     * Gets the placeholder text.
     *
     * @return the placeholder attribute, or empty string if not set
     */
    public String getPlaceholder() {
        String placeholder = getAttribute("placeholder");
        return placeholder != null ? placeholder : "";
    }

    /**
     * Checks if the field is empty.
     *
     * @return true if value is null or empty
     */
    public boolean isEmpty() {
        String value = getValue();
        return value == null || value.isEmpty();
    }

    // ==================== State Checking ====================

    /**
     * Checks if the field is read-only.
     *
     * @return true if readonly attribute is present
     */
    public boolean isReadOnly() {
        String readonly = getAttribute("readonly");
        return readonly != null;
    }

    /**
     * Gets the maximum length allowed in the field.
     *
     * @return the maxlength value, or -1 if not set
     */
    public int getMaxLength() {
        String maxLength = getAttribute("maxlength");
        if (maxLength != null) {
            try {
                return Integer.parseInt(maxLength);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Gets the input type (text, password, email, etc.).
     *
     * @return the type attribute, or "text" if not set
     */
    public String getInputType() {
        String type = getAttribute("type");
        return type != null ? type : "text";
    }

    // ==================== Keyboard Operations ====================

    /**
     * Presses the Enter key.
     *
     * @return this element for chaining
     */
    public TextField pressEnter() {
        if (isNull()) {
            log.warn("Cannot press Enter - element is null");
            return this;
        }
        try {
            baseElement.sendKeys(Keys.ENTER);
            log.debug("Pressed Enter");
        } catch (Exception e) {
            log.error("Error pressing Enter: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Presses the Tab key.
     *
     * @return this element for chaining
     */
    public TextField pressTab() {
        if (isNull()) {
            log.warn("Cannot press Tab - element is null");
            return this;
        }
        try {
            baseElement.sendKeys(Keys.TAB);
            log.debug("Pressed Tab");
        } catch (Exception e) {
            log.error("Error pressing Tab: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Presses the Escape key.
     *
     * @return this element for chaining
     */
    public TextField pressEscape() {
        if (isNull()) {
            log.warn("Cannot press Escape - element is null");
            return this;
        }
        try {
            baseElement.sendKeys(Keys.ESCAPE);
            log.debug("Pressed Escape");
        } catch (Exception e) {
            log.error("Error pressing Escape: {}", e.getMessage());
        }
        return this;
    }

    /**
     * Presses the Backspace key.
     *
     * @return this element for chaining
     */
    public TextField pressBackspace() {
        if (isNull()) {
            log.warn("Cannot press Backspace - element is null");
            return this;
        }
        try {
            baseElement.sendKeys(Keys.BACK_SPACE);
            log.debug("Pressed Backspace");
        } catch (Exception e) {
            log.error("Error pressing Backspace: {}", e.getMessage());
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("TextField[locator=%s, value=%s, isNull=%s]",
            locator, getValue(), isNull());
    }
}
