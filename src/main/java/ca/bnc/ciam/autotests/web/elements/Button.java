package ca.bnc.ciam.autotests.web.elements;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

/**
 * Button wrapper class for button elements.
 * Extends Element with button-specific methods.
 *
 * Features:
 * - Click operations (standard, JS, double-click, right-click)
 * - Submit form support
 * - Label/text retrieval
 * - Disabled state checking
 * - Click and wait support
 *
 * Usage:
 * <pre>
 * Button submitBtn = new Button(driver, webElement, locator);
 * if (!submitBtn.isDisabled()) {
 *     submitBtn.click();
 * }
 * </pre>
 */
@Slf4j
public class Button extends Element {

    /**
     * Creates a Button wrapper.
     *
     * @param driver  The WebDriver instance
     * @param element The WebElement to wrap (can be null)
     * @param locator The By locator used to find this element
     */
    public Button(WebDriver driver, WebElement element, By locator) {
        super(driver, element, locator);
    }

    // ==================== Click Operations ====================

    @Override
    public Button click() {
        super.click();
        return this;
    }

    @Override
    public Button jsClick() {
        super.jsClick();
        return this;
    }

    @Override
    public Button doubleClick() {
        super.doubleClick();
        return this;
    }

    @Override
    public Button rightClick() {
        super.rightClick();
        return this;
    }

    /**
     * Submits the form associated with this button.
     *
     * @return this element for chaining
     */
    public Button submit() {
        if (isNull()) {
            log.warn("Cannot submit - element is null");
            return this;
        }
        try {
            baseElement.submit();
            log.debug("Submitted form");
        } catch (Exception e) {
            log.error("Error submitting form: {}", e.getMessage());
            throw e;
        }
        return this;
    }

    /**
     * Clicks the button and waits for a specified duration.
     *
     * @param waitMillis milliseconds to wait after clicking
     * @return this element for chaining
     */
    public Button clickAndWait(long waitMillis) {
        click();
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Wait interrupted");
        }
        return this;
    }

    /**
     * Clicks the button using Actions (useful for stubborn buttons).
     *
     * @return this element for chaining
     */
    public Button actionsClick() {
        if (isNull()) {
            log.warn("Cannot actions click - element is null");
            return this;
        }
        try {
            new Actions(driver).click(baseElement).perform();
            log.debug("Actions clicked button");
        } catch (Exception e) {
            log.error("Error actions clicking: {}", e.getMessage());
            throw e;
        }
        return this;
    }

    // ==================== Label/Text Retrieval ====================

    /**
     * Gets the button label/text.
     *
     * @return the button text, or value attribute if text is empty
     */
    public String getLabel() {
        String text = getText();
        if (text != null && !text.isEmpty()) {
            return text;
        }
        // Fallback to value attribute (for input type="submit")
        String value = getValue();
        return value != null ? value : "";
    }

    // ==================== State Checking ====================

    /**
     * Checks if the button is disabled.
     *
     * @return true if disabled attribute is present or element is not enabled
     */
    public boolean isDisabled() {
        if (isNull()) {
            return true;
        }
        String disabled = getAttribute("disabled");
        if (disabled != null) {
            return true;
        }
        return !isEnabled();
    }

    /**
     * Checks if the button has the "aria-disabled" attribute set to true.
     *
     * @return true if aria-disabled is "true"
     */
    public boolean isAriaDisabled() {
        String ariaDisabled = getAttribute("aria-disabled");
        return "true".equalsIgnoreCase(ariaDisabled);
    }

    /**
     * Checks if the button is a submit button.
     *
     * @return true if type is "submit"
     */
    public boolean isSubmitButton() {
        String type = getAttribute("type");
        return "submit".equalsIgnoreCase(type);
    }

    /**
     * Checks if the button is a reset button.
     *
     * @return true if type is "reset"
     */
    public boolean isResetButton() {
        String type = getAttribute("type");
        return "reset".equalsIgnoreCase(type);
    }

    /**
     * Gets the button type.
     *
     * @return the type attribute (submit, button, reset), or "button" if not set
     */
    public String getButtonType() {
        String type = getAttribute("type");
        return type != null ? type : "button";
    }

    @Override
    public String toString() {
        return String.format("Button[locator=%s, label=%s, isDisabled=%s, isNull=%s]",
            locator, getLabel(), isDisabled(), isNull());
    }
}
