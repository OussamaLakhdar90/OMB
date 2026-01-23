package ca.bnc.ciam.autotests.web.config;

/**
 * Supported browser types for WebDriver.
 */
public enum BrowserType {
    CHROME("chrome"),
    FIREFOX("firefox"),
    EDGE("edge"),
    SAFARI("safari"),
    IE("ie");

    private final String name;

    BrowserType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Get BrowserType from string name (case-insensitive).
     */
    public static BrowserType fromString(String name) {
        if (name == null || name.isEmpty()) {
            return CHROME; // Default
        }
        for (BrowserType type : values()) {
            if (type.name.equalsIgnoreCase(name) || type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown browser type: " + name);
    }
}
