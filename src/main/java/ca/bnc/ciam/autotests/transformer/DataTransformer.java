package ca.bnc.ciam.autotests.transformer;

import lombok.extern.slf4j.Slf4j;

/**
 * Transforms test data values.
 * Handles special prefixes like $sensitive: for environment variable substitution.
 */
@Slf4j
public class DataTransformer implements IDataTransformer {

    private static final String SENSITIVE_MARKER = "$sensitive:";

    @Override
    public String transform(String value) {
        if (value == null || !value.startsWith(SENSITIVE_MARKER)) {
            return value;
        }

        int delimiterIndex = value.lastIndexOf(':');
        if (delimiterIndex == -1 || delimiterIndex == value.length() - 1) {
            log.warn("Invalid sensitive value format: '{}'", value);
            return value;
        }

        String key = value.substring(delimiterIndex + 1).trim();
        String envVariableValue = System.getenv(key);

        if (envVariableValue != null) {
            return envVariableValue;
        }

        log.warn("Environment variable '{}' not found.", key);
        return value;
    }

    /**
     * Check if a value is marked as sensitive.
     *
     * @param value The value to check
     * @return True if the value is marked as sensitive
     */
    public boolean isSensitive(String value) {
        return value != null && value.startsWith(SENSITIVE_MARKER);
    }

    /**
     * Get the environment variable key from a sensitive value.
     *
     * @param value The sensitive value
     * @return The environment variable key, or null if not a sensitive value
     */
    public String getSensitiveKey(String value) {
        if (!isSensitive(value)) {
            return null;
        }

        int delimiterIndex = value.lastIndexOf(':');
        if (delimiterIndex == -1 || delimiterIndex == value.length() - 1) {
            return null;
        }

        return value.substring(delimiterIndex + 1).trim();
    }
}
