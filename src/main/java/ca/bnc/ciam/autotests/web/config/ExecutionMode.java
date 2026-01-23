package ca.bnc.ciam.autotests.web.config;

/**
 * Test execution mode - local or remote (SauceLabs).
 */
public enum ExecutionMode {
    LOCAL("local"),
    SAUCELABS("saucelabs"),
    REMOTE("remote");

    private final String name;

    ExecutionMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Get ExecutionMode from string name (case-insensitive).
     */
    public static ExecutionMode fromString(String name) {
        if (name == null || name.isEmpty()) {
            return LOCAL; // Default
        }
        for (ExecutionMode mode : values()) {
            if (mode.name.equalsIgnoreCase(name) || mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown execution mode: " + name);
    }
}
