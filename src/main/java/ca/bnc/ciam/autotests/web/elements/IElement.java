package ca.bnc.ciam.autotests.web.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Interface for element wrappers that provide enhanced functionality over raw WebElement.
 * All element wrapper classes (Element, TextField, Button, CheckBox, Image) should implement this interface.
 *
 * This interface provides:
 * - Null-safe operations
 * - State checking methods (isNull, isDisplayed, isEnabled, isSelected)
 * - Text and attribute retrieval
 * - Click operations (standard and JavaScript)
 * - Scroll and hover operations
 * - Wait operations
 * - Chaining support for fluent API
 */
public interface IElement {

    // ==================== State Checking ====================

    /**
     * Checks if the underlying WebElement is null.
     *
     * @return true if element is null, false otherwise
     */
    boolean isNull();

    /**
     * Checks if element is displayed on the page.
     *
     * @return true if element is displayed, false if null or not displayed
     */
    boolean isDisplayed();

    /**
     * Checks if element is enabled (not disabled).
     *
     * @return true if element is enabled, false if null or disabled
     */
    boolean isEnabled();

    /**
     * Checks if element is selected (for checkboxes, radio buttons, options).
     *
     * @return true if selected, false if null or not selected
     */
    boolean isSelected();

    // ==================== Text and Attribute Retrieval ====================

    /**
     * Gets the visible text of the element.
     *
     * @return the text content, or empty string if null
     */
    String getText();

    /**
     * Gets an attribute value from the element.
     *
     * @param attributeName the name of the attribute
     * @return the attribute value, or null if element is null or attribute doesn't exist
     */
    String getAttribute(String attributeName);

    /**
     * Gets the tag name of the element.
     *
     * @return the tag name (e.g., "input", "button"), or empty string if null
     */
    String getTagName();

    // ==================== Click Operations ====================

    /**
     * Clicks the element using standard WebDriver click.
     *
     * @return this element for method chaining
     */
    IElement click();

    /**
     * Clicks element using JavaScript (useful when regular click fails due to overlays).
     *
     * @return this element for method chaining
     */
    IElement jsClick();

    // ==================== Scroll Operations ====================

    /**
     * Scrolls the element into view (centered in viewport).
     *
     * @return this element for method chaining
     */
    IElement scrollIntoView();

    // ==================== Mouse Operations ====================

    /**
     * Hovers over the element using Selenium Actions.
     *
     * @return this element for method chaining
     */
    IElement hover();

    // ==================== Wait Operations ====================

    /**
     * Waits for element to become visible.
     *
     * @param timeoutSeconds maximum time to wait in seconds
     * @return this element for method chaining
     */
    IElement waitUntilVisible(long timeoutSeconds);

    /**
     * Waits for element to become clickable.
     *
     * @param timeoutSeconds maximum time to wait in seconds
     * @return this element for method chaining
     */
    IElement waitUntilClickable(long timeoutSeconds);

    // ==================== Debug Operations ====================

    /**
     * Highlights the element temporarily (useful for visual debugging).
     *
     * @return this element for method chaining
     */
    IElement highlight();

    // ==================== Underlying Objects ====================

    /**
     * Gets the WebDriver instance.
     *
     * @return the WebDriver
     */
    WebDriver getDriver();

    /**
     * Gets the underlying WebElement (may be null).
     *
     * @return the WebElement or null
     */
    WebElement getBaseElement();

    /**
     * Gets the locator used to find this element.
     *
     * @return the By locator
     */
    By getLocator();
}
