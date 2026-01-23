package ca.bnc.ciam.autotests.web.elements;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * CheckBox wrapper class for checkbox input elements.
 * Extends Element with checkbox-specific methods.
 *
 * Features:
 * - Check/uncheck operations
 * - Toggle support
 * - State checking (isChecked, isIndeterminate)
 * - Set checked state
 *
 * Usage:
 * <pre>
 * CheckBox termsCheckbox = new CheckBox(driver, webElement, locator);
 * termsCheckbox.check();
 *
 * if (termsCheckbox.isChecked()) {
 *     // proceed
 * }
 * </pre>
 */
@Slf4j
public class CheckBox extends Element {

    /**
     * Creates a CheckBox wrapper.
     *
     * @param driver  The WebDriver instance
     * @param element The WebElement to wrap (can be null)
     * @param locator The By locator used to find this element
     */
    public CheckBox(WebDriver driver, WebElement element, By locator) {
        super(driver, element, locator);
    }

    // ==================== Check Operations ====================

    /**
     * Checks the checkbox (only if not already checked).
     *
     * @return this element for chaining
     */
    public CheckBox check() {
        if (isNull()) {
            log.warn("Cannot check - element is null");
            return this;
        }
        if (!isChecked()) {
            click();
            log.debug("Checked checkbox");
        }
        return this;
    }

    /**
     * Unchecks the checkbox (only if currently checked).
     *
     * @return this element for chaining
     */
    public CheckBox uncheck() {
        if (isNull()) {
            log.warn("Cannot uncheck - element is null");
            return this;
        }
        if (isChecked()) {
            click();
            log.debug("Unchecked checkbox");
        }
        return this;
    }

    /**
     * Toggles the checkbox state.
     *
     * @return this element for chaining
     */
    public CheckBox toggle() {
        if (isNull()) {
            log.warn("Cannot toggle - element is null");
            return this;
        }
        click();
        log.debug("Toggled checkbox");
        return this;
    }

    /**
     * Sets the checkbox to a specific state.
     *
     * @param checked true to check, false to uncheck
     * @return this element for chaining
     */
    public CheckBox setChecked(boolean checked) {
        if (checked) {
            return check();
        } else {
            return uncheck();
        }
    }

    // ==================== State Checking ====================

    /**
     * Checks if the checkbox is checked.
     *
     * @return true if checked, false otherwise
     */
    public boolean isChecked() {
        if (isNull()) {
            return false;
        }
        // First try the standard isSelected method
        if (isSelected()) {
            return true;
        }
        // Fallback: check the "checked" attribute
        String checked = getAttribute("checked");
        if (checked != null) {
            return true;
        }
        // Fallback: check aria-checked attribute
        String ariaChecked = getAttribute("aria-checked");
        return "true".equalsIgnoreCase(ariaChecked);
    }

    /**
     * Checks if the checkbox is in indeterminate state (partially checked).
     *
     * @return true if indeterminate
     */
    public boolean isIndeterminate() {
        if (isNull()) {
            return false;
        }
        String ariaChecked = getAttribute("aria-checked");
        if ("mixed".equalsIgnoreCase(ariaChecked)) {
            return true;
        }
        // Try checking via JavaScript
        try {
            Object result = ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("return arguments[0].indeterminate;", baseElement);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.debug("Could not check indeterminate state: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Gets the label text associated with this checkbox (if using label element).
     *
     * @return the label text, or empty string if not found
     */
    public String getLabel() {
        if (isNull()) {
            return "";
        }
        // Try to find label by "for" attribute matching checkbox id
        String id = getAttribute("id");
        if (id != null && !id.isEmpty()) {
            try {
                WebElement label = driver.findElement(By.cssSelector("label[for='" + id + "']"));
                return label.getText();
            } catch (Exception e) {
                // Label not found by for attribute
            }
        }
        // Try to find parent label
        try {
            WebElement parent = baseElement.findElement(By.xpath("./parent::label"));
            return parent.getText();
        } catch (Exception e) {
            // No parent label
        }
        return "";
    }

    /**
     * Checks if the checkbox is required.
     *
     * @return true if required attribute is present
     */
    public boolean isRequired() {
        String required = getAttribute("required");
        return required != null;
    }

    @Override
    public String toString() {
        return String.format("CheckBox[locator=%s, isChecked=%s, isNull=%s]",
            locator, isChecked(), isNull());
    }
}
