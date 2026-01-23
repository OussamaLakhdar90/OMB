package ca.bnc.ciam.autotests.web.elements;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Image wrapper class for image elements.
 * Extends Element with image-specific methods.
 *
 * Features:
 * - Source URL retrieval (src, currentSrc)
 * - Alt text retrieval
 * - Dimension retrieval (natural and displayed)
 * - Load state checking (isLoaded, isBroken)
 *
 * Usage:
 * <pre>
 * Image logo = new Image(driver, webElement, locator);
 * if (logo.isLoaded()) {
 *     System.out.println("Image loaded: " + logo.getSrc());
 * }
 * </pre>
 */
@Slf4j
public class Image extends Element {

    /**
     * Creates an Image wrapper.
     *
     * @param driver  The WebDriver instance
     * @param element The WebElement to wrap (can be null)
     * @param locator The By locator used to find this element
     */
    public Image(WebDriver driver, WebElement element, By locator) {
        super(driver, element, locator);
    }

    // ==================== Source Retrieval ====================

    /**
     * Gets the image source URL.
     *
     * @return the src attribute value, or empty string if null
     */
    public String getSrc() {
        if (isNull()) {
            return "";
        }
        String src = getAttribute("src");
        return src != null ? src : "";
    }

    /**
     * Gets the current source (handles lazy loading/responsive images).
     *
     * @return the currentSrc value, or falls back to src
     */
    public String getCurrentSrc() {
        if (isNull()) {
            return "";
        }
        try {
            Object currentSrc = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].currentSrc;", baseElement);
            return currentSrc != null ? currentSrc.toString() : getSrc();
        } catch (Exception e) {
            return getSrc();
        }
    }

    /**
     * Gets the alternative text for the image.
     *
     * @return the alt attribute value, or empty string if not set
     */
    public String getAlt() {
        if (isNull()) {
            return "";
        }
        String alt = getAttribute("alt");
        return alt != null ? alt : "";
    }

    /**
     * Gets the image title.
     *
     * @return the title attribute value, or empty string if not set
     */
    public String getTitle() {
        if (isNull()) {
            return "";
        }
        String title = getAttribute("title");
        return title != null ? title : "";
    }

    // ==================== Dimension Retrieval ====================

    /**
     * Gets the natural width of the image (actual image width).
     *
     * @return the natural width, or 0 if cannot be determined
     */
    public int getNaturalWidth() {
        if (isNull()) {
            return 0;
        }
        try {
            Object width = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].naturalWidth;", baseElement);
            return width != null ? ((Number) width).intValue() : 0;
        } catch (Exception e) {
            log.debug("Error getting natural width: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the natural height of the image (actual image height).
     *
     * @return the natural height, or 0 if cannot be determined
     */
    public int getNaturalHeight() {
        if (isNull()) {
            return 0;
        }
        try {
            Object height = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].naturalHeight;", baseElement);
            return height != null ? ((Number) height).intValue() : 0;
        } catch (Exception e) {
            log.debug("Error getting natural height: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the displayed width of the image (CSS width).
     *
     * @return the displayed width, or 0 if cannot be determined
     */
    public int getDisplayedWidth() {
        if (isNull()) {
            return 0;
        }
        try {
            return baseElement.getSize().getWidth();
        } catch (Exception e) {
            log.debug("Error getting displayed width: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Gets the displayed height of the image (CSS height).
     *
     * @return the displayed height, or 0 if cannot be determined
     */
    public int getDisplayedHeight() {
        if (isNull()) {
            return 0;
        }
        try {
            return baseElement.getSize().getHeight();
        } catch (Exception e) {
            log.debug("Error getting displayed height: {}", e.getMessage());
            return 0;
        }
    }

    // ==================== Load State Checking ====================

    /**
     * Checks if the image has loaded successfully.
     *
     * @return true if image is complete and has natural dimensions
     */
    public boolean isLoaded() {
        if (isNull()) {
            return false;
        }
        try {
            Object complete = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].complete;", baseElement);
            if (complete == null || !(Boolean) complete) {
                return false;
            }
            // Also check that it has actual dimensions (not a broken image)
            return getNaturalWidth() > 0 && getNaturalHeight() > 0;
        } catch (Exception e) {
            log.debug("Error checking if image is loaded: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the image is broken (failed to load).
     *
     * @return true if image failed to load
     */
    public boolean isBroken() {
        if (isNull()) {
            return true;
        }
        try {
            Object complete = ((JavascriptExecutor) driver)
                .executeScript("return arguments[0].complete;", baseElement);
            if (complete == null || !(Boolean) complete) {
                return false; // Still loading, not broken yet
            }
            // Broken images have 0 natural dimensions
            return getNaturalWidth() == 0 || getNaturalHeight() == 0;
        } catch (Exception e) {
            log.debug("Error checking if image is broken: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Waits for the image to load.
     *
     * @param timeoutSeconds maximum time to wait
     * @return this element for chaining
     */
    public Image waitUntilLoaded(long timeoutSeconds) {
        if (isNull()) {
            log.warn("Cannot wait - element is null");
            return this;
        }
        long endTime = System.currentTimeMillis() + (timeoutSeconds * 1000);
        while (System.currentTimeMillis() < endTime) {
            if (isLoaded()) {
                log.debug("Image loaded");
                return this;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.warn("Image not loaded after {} seconds", timeoutSeconds);
        return this;
    }

    @Override
    public String toString() {
        return String.format("Image[locator=%s, src=%s, alt=%s, isLoaded=%s, isNull=%s]",
            locator,
            getSrc().length() > 50 ? getSrc().substring(0, 50) + "..." : getSrc(),
            getAlt(),
            isLoaded(),
            isNull());
    }
}
