package ca.bnc.ciam.autotests.transformer;

import lombok.extern.slf4j.Slf4j;

/**
 * Transforms test data values.
 * Handles special prefixes like $sensitive: for environment variable substitution.
 */
@Slf4j
public class DataTransformer implements IDataTransformer {

    private static final String SENSITIVE_MARKER = "$sensitive:";
    private static final String LEGACY_SENSITIVE_MARKER = "$en$itive:";

    @Override
    public String transform(String value) {
        if (value == null) {
            return value;
        }

        // Check for modern marker
        if (value.startsWith(SENSITIVE_MARKER)) {
            return resolveEnvVariable(value, SENSITIVE_MARKER);
        }

        // Check for legacy marker
        if (value.startsWith(LEGACY_SENSITIVE_MARKER)) {
            return resolveEnvVariable(value, LEGACY_SENSITIVE_MARKER);
        }

        return value;
    }

    /**
     * Resolve environment variable from sensitive value.
     *
     * @param value  The sensitive value string
     * @param marker The marker prefix used
     * @return The resolved value from environment, or original if not found
     */
    private String resolveEnvVariable(String value, String marker) {
        String key = value.substring(marker.length()).trim();

        if (key.isEmpty()) {
            log.warn("Invalid sensitive value format - empty key: '{}'", value);
            return value;
        }

        String envVariableValue = System.getenv(key);

        if (envVariableValue != null) {
            log.debug("Resolved sensitive value for key '{}' from environment", key);
            return envVariableValue;
        }

        // Also try system property as fallback
        String sysPropValue = System.getProperty(key);
        if (sysPropValue != null) {
            log.debug("Resolved sensitive value for key '{}' from system property", key);
            return sysPropValue;
        }

        log.warn("Environment variable '{}' not found. Value will not be transformed.", key);
        return value;
    }

    /**
     * Check if a value is marked as sensitive.
     *
     * @param value The value to check
     * @return True if the value is marked as sensitive
     */
    public boolean isSensitive(String value) {
        return value != null && (value.startsWith(SENSITIVE_MARKER) || value.startsWith(LEGACY_SENSITIVE_MARKER));
    }

    /**
     * Get the environment variable key from a sensitive value.
     *
     * @param value The sensitive value
     * @return The environment variable key, or null if not a sensitive value
     */
    public String getSensitiveKey(String value) {
        if (value == null) {
            return null;
        }

        if (value.startsWith(SENSITIVE_MARKER)) {
            return value.substring(SENSITIVE_MARKER.length()).trim();
        }

        if (value.startsWith(LEGACY_SENSITIVE_MARKER)) {
            return value.substring(LEGACY_SENSITIVE_MARKER.length()).trim();
        }

        return null;
    }
}
