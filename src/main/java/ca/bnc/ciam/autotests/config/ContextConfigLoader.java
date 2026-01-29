package ca.bnc.ciam.autotests.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads configuration from context.json for pipeline execution.
 *
 * <p>Configuration is structured as:</p>
 * <pre>
 * {
 *   "_saucelabs": { ... },           // Global SauceLabs settings
 *   "staging-ta": {                   // Environment
 *     "bnc.data.manager": "...",      // Environment-level properties
 *     "chrome-fr": {                  // ConfigKey
 *       "bnc.web.gui.lang": "fr",
 *       "bnc.web.app.url": "...",
 *       "bnc.web.browsers.config": "..."
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Merge order (lower to higher priority):</p>
 * <ol>
 *   <li>_saucelabs (global)</li>
 *   <li>environment properties</li>
 *   <li>configKey properties (highest priority, can override)</li>
 * </ol>
 *
 * <p>This class is only used in pipeline mode (when testEnvironment is provided).
 * Local execution continues to use debug_config.json.</p>
 */
@Slf4j
public class ContextConfigLoader {

    private static final String CONTEXT_FILE_PATH = "src/test/resources/contexts/context.json";
    private static final String SAUCELABS_KEY = "_saucelabs";

    private static ContextConfigLoader instance;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Map<String, Object> contextData;
    private boolean loaded = false;

    private ContextConfigLoader() {
        // Private constructor for singleton
    }

    /**
     * Get singleton instance.
     */
    public static synchronized ContextConfigLoader getInstance() {
        if (instance == null) {
            instance = new ContextConfigLoader();
        }
        return instance;
    }

    /**
     * Load configuration for the given environment and configKey.
     * Merges _saucelabs + environment props + configKey props.
     * Sets all merged properties as system properties.
     *
     * @param testEnvironment The environment name (e.g., "staging-ta")
     * @param configKey The config key from XML (e.g., "chrome-fr")
     */
    public void loadConfig(String testEnvironment, String configKey) {
        if (testEnvironment == null || testEnvironment.isEmpty()) {
            log.debug("testEnvironment is null/empty - skipping context config loading");
            return;
        }

        if (configKey == null || configKey.isEmpty()) {
            log.warn("configKey is null/empty for testEnvironment={} - skipping context config loading",
                     testEnvironment);
            return;
        }

        // Load context.json if not already loaded
        if (!loaded) {
            loadContextFile();
        }

        if (contextData == null) {
            log.error("Context data not loaded - cannot configure for env={}, configKey={}",
                      testEnvironment, configKey);
            return;
        }

        // Merge configuration
        Map<String, String> mergedConfig = getMergedConfig(testEnvironment, configKey);

        if (mergedConfig.isEmpty()) {
            log.warn("No configuration found for env={}, configKey={}", testEnvironment, configKey);
            return;
        }

        // Set all properties as system properties
        for (Map.Entry<String, String> entry : mergedConfig.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
            log.debug("Set system property: {} = {}", entry.getKey(), entry.getValue());
        }

        log.info("Loaded configuration for env={}, configKey={} ({} properties)",
                 testEnvironment, configKey, mergedConfig.size());
    }

    /**
     * Get merged configuration without setting system properties.
     *
     * @param testEnvironment The environment name
     * @param configKey The config key
     * @return Merged properties map
     */
    public Map<String, String> getMergedConfig(String testEnvironment, String configKey) {
        Map<String, String> merged = new HashMap<>();

        if (contextData == null) {
            loadContextFile();
        }

        if (contextData == null) {
            return merged;
        }

        // 1. Add _saucelabs properties (global)
        Object saucelabsObj = contextData.get(SAUCELABS_KEY);
        if (saucelabsObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> saucelabs = (Map<String, Object>) saucelabsObj;
            addPropertiesToMap(saucelabs, merged);
            log.debug("Added {} _saucelabs properties", saucelabs.size());
        }

        // 2. Get environment data
        Object envObj = contextData.get(testEnvironment);
        if (!(envObj instanceof Map)) {
            log.warn("Environment '{}' not found in context.json", testEnvironment);
            return merged;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> envData = (Map<String, Object>) envObj;

        // 3. Add environment-level properties (not nested objects)
        for (Map.Entry<String, Object> entry : envData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip configKey entries (nested objects)
            if (!(value instanceof Map)) {
                merged.put(key, String.valueOf(value));
                log.debug("Added env property: {} = {}", key, value);
            }
        }

        // 4. Add configKey properties (highest priority - can override)
        Object configKeyObj = envData.get(configKey);
        if (configKeyObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> configKeyData = (Map<String, Object>) configKeyObj;
            addPropertiesToMap(configKeyData, merged);
            log.debug("Added {} configKey properties for '{}'", configKeyData.size(), configKey);
        } else {
            log.warn("ConfigKey '{}' not found in environment '{}'", configKey, testEnvironment);
        }

        return merged;
    }

    /**
     * Check if running in pipeline mode.
     * Pipeline mode is when testEnvironment system property is set.
     */
    public boolean isPipelineMode() {
        String testEnv = System.getProperty("testEnvironment");
        return testEnv != null && !testEnv.isEmpty();
    }

    /**
     * Get the current test environment.
     */
    public String getTestEnvironment() {
        return System.getProperty("testEnvironment");
    }

    /**
     * Load context.json file.
     */
    private void loadContextFile() {
        File contextFile = new File(CONTEXT_FILE_PATH);

        if (!contextFile.exists()) {
            log.warn("Context file not found at: {}", CONTEXT_FILE_PATH);
            loaded = true;
            return;
        }

        try {
            contextData = objectMapper.readValue(contextFile, new TypeReference<>() {});
            loaded = true;
            log.info("Loaded context.json from {}", CONTEXT_FILE_PATH);
        } catch (IOException e) {
            log.error("Failed to load context.json: {}", e.getMessage());
            loaded = true;
        }
    }

    /**
     * Add properties from source map to target map.
     */
    private void addPropertiesToMap(Map<String, Object> source, Map<String, String> target) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            // Only add simple values, not nested objects
            if (!(value instanceof Map)) {
                target.put(entry.getKey(), String.valueOf(value));
            }
        }
    }

    /**
     * Reset the loader (for testing purposes).
     */
    public static void reset() {
        instance = null;
    }
}
