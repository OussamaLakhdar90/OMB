package ca.bnc.ciam.autotests.data;

import ca.bnc.ciam.autotests.exception.TestDataException;
import ca.bnc.ciam.autotests.transformer.DataTransformer;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;

/**
 * Fluent API for accessing test data from JSON files.
 * Provides methods to filter and access test data by source, key, and index.
 */
@Slf4j
public class TestData {

    private Map<String, String> testDataMap;
    private String sourceFile;
    private int index = 1;
    private static final DataTransformer dataTransformer = new DataTransformer();

    /**
     * Create a TestData instance with the test data map.
     */
    public TestData() {
    }

    /**
     * Create a TestData instance with the provided data map.
     */
    public TestData(Map<String, String> testDataMap) {
        this.testDataMap = testDataMap;
    }

    /**
     * Set the test data map.
     */
    public TestData withData(Map<String, String> testDataMap) {
        this.testDataMap = testDataMap;
        return this;
    }

    /**
     * Filter data from a specific source file.
     *
     * @param sourceFile The source file name (e.g., "users.json")
     * @return This TestData instance for chaining
     */
    public TestData from(String sourceFile) {
        this.sourceFile = sourceFile;
        return this;
    }

    /**
     * Filter data for a specific index (1-based).
     *
     * @param index The data index (1-based)
     * @return This TestData instance for chaining
     */
    public TestData forIndex(int index) {
        this.index = index;
        return this;
    }

    /**
     * Get a string value for the specified key.
     * Automatically transforms sensitive values (e.g., $sensitive:ENV_VAR_NAME)
     * to their actual values from environment variables.
     *
     * @param key The data key
     * @return The string value (transformed if sensitive)
     */
    public String getForKey(String key) {
        String fullKey = buildKey(key);
        String value = testDataMap.get(fullKey);
        if (value == null) {
            log.warn("Test data not found for key: {}", fullKey);
            return null;
        }
        // Transform sensitive values to resolve environment variables
        return dataTransformer.transform(value);
    }

    /**
     * Get a string value for the specified key with default.
     *
     * @param key          The data key
     * @param defaultValue The default value if not found
     * @return The string value or default
     */
    public String getForKey(String key, String defaultValue) {
        String value = getForKey(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a boolean value for the specified key.
     *
     * @param key The data key
     * @return The boolean value
     */
    public Boolean getBoolean(String key) {
        String value = getForKey(key);
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * Get a boolean value for the specified key with default.
     *
     * @param key          The data key
     * @param defaultValue The default value if not found
     * @return The boolean value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get an integer value for the specified key.
     *
     * @param key The data key
     * @return The integer value
     */
    public Integer getInteger(String key) {
        String value = getForKey(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new TestDataException("Cannot parse integer for key: " + key, e);
        }
    }

    /**
     * Get an integer value for the specified key with default.
     *
     * @param key          The data key
     * @param defaultValue The default value if not found
     * @return The integer value or default
     */
    public int getInteger(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a long value for the specified key.
     *
     * @param key The data key
     * @return The long value
     */
    public Long getLong(String key) {
        String value = getForKey(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new TestDataException("Cannot parse long for key: " + key, e);
        }
    }

    /**
     * Get a double value for the specified key.
     *
     * @param key The data key
     * @return The double value
     */
    public Double getDouble(String key) {
        String value = getForKey(key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new TestDataException("Cannot parse double for key: " + key, e);
        }
    }

    /**
     * Get a JSONObject value for the specified key.
     * The key should be prefixed with "as-json-" in the data file.
     *
     * @param key The data key (without "as-json-" prefix)
     * @return The JSONObject value
     */
    public JSONObject getJSONObject(String key) {
        String value = getForKey("as-json-" + key);
        if (value == null) {
            value = getForKey(key);
        }
        if (value == null) {
            return null;
        }
        try {
            return new JSONObject(value);
        } catch (Exception e) {
            throw new TestDataException("Cannot parse JSONObject for key: " + key, e);
        }
    }

    /**
     * Get a JSONArray value for the specified key.
     * The key should be prefixed with "as-json-" in the data file.
     *
     * @param key The data key (without "as-json-" prefix)
     * @return The JSONArray value
     */
    public JSONArray getJSONArray(String key) {
        String value = getForKey("as-json-" + key);
        if (value == null) {
            value = getForKey(key);
        }
        if (value == null) {
            return null;
        }
        try {
            return new JSONArray(value);
        } catch (Exception e) {
            throw new TestDataException("Cannot parse JSONArray for key: " + key, e);
        }
    }

    /**
     * Get the descriptor for the current test data set.
     *
     * @return The descriptor value
     */
    public String getDescriptor() {
        return testDataMap != null ? testDataMap.get("descriptor") : null;
    }

    /**
     * Get the comment for the current test data set.
     *
     * @return The comment value
     */
    public String getComment() {
        return testDataMap != null ? testDataMap.get("comment") : null;
    }

    /**
     * Check if a key exists in the test data.
     *
     * @param key The data key
     * @return True if key exists
     */
    public boolean hasKey(String key) {
        String fullKey = buildKey(key);
        return testDataMap != null && testDataMap.containsKey(fullKey);
    }

    /**
     * Get the underlying data map.
     *
     * @return The test data map
     */
    public Map<String, String> getDataMap() {
        return testDataMap;
    }

    /**
     * Build the full key from source file, key, and index.
     * Format: "{sourceFile}:{key}:{index}"
     */
    private String buildKey(String key) {
        if (sourceFile == null) {
            return key;
        }
        return String.format("%s:%s:%d", sourceFile, key, index);
    }

    @Override
    public String toString() {
        return "TestData{" +
                "sourceFile='" + sourceFile + '\'' +
                ", index=" + index +
                ", descriptor='" + getDescriptor() + '\'' +
                '}';
    }
}
