package ca.bnc.ciam.autotests.visual.model;

/**
 * Defines the type of screenshot to capture for visual comparison.
 */
public enum ScreenshotType {

    /**
     * Capture the entire page including scrollable content
     */
    FULL_PAGE,

    /**
     * Capture only the visible viewport
     */
    VIEWPORT,

    /**
     * Capture a specific element
     */
    ELEMENT,

    /**
     * Capture both full page and element (when selector is provided)
     */
    BOTH
}
