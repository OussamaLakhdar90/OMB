package ca.bnc.ciam.autotests.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads browser configuration from JSON files.
 * Supports both pipeline mode (bnc.web.browsers.config) and local mode (debug_config.json).
 *
 * <p>Pipeline mode loads from config files like config_chrome_win10.json:
 * <pre>
 * {
 *   "browsers": [{
 *     "browserName": "chrome",
 *     "platformName": "WIN10",
 *     "sauce:options": {
 *       "extendedDebugging": true,
 *       "screenResolution": "1920x1080",
 *       "parentTunnel": "TestAdmin",
 *       "tunnelIdentifier": "SauceConnect"
 *     },
 *     "goog:chromeOptions": {
 *       "args": ["--ignore-certificate-errors", "--disable-notifications"]
 *     }
 *   }]
 * }
 * </pre>
 *
 * <p>Local mode loads from debug_config.json:
 * <pre>
 * {
 *   "browser": "chrome",
 *   "cap": {
 *     "headless": false,
 *     "window_size": "1920x1080",
 *     "disable_notifications": true
 *   }
 * }
 * </pre>
 */
@Slf4j
public class BrowserConfigLoader {

    private static final String DEBUG_CONFIG_PATH = "src/test/resources/debug_config.json";
    private static final String CONFIG_BASE_PATH = "src/test/resources/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static BrowserConfigLoader instance;

    private JsonNode debugConfig;
    private final Map<String, BrowserConfig> browserConfigCache = new HashMap<>();

    private BrowserConfigLoader() {
        loadDebugConfig();
    }

    public static synchronized BrowserConfigLoader getInstance() {
        if (instance == null) {
            instance = new BrowserConfigLoader();
        }
        return instance;
    }

    /**
     * Load debug_config.json for local execution.
     */
    private void loadDebugConfig() {
        File debugFile = new File(DEBUG_CONFIG_PATH);
        if (debugFile.exists()) {
            try {
                debugConfig = objectMapper.readTree(debugFile);
                log.info("Loaded debug_config.json");
            } catch (IOException e) {
                log.warn("Failed to load debug_config.json: {}", e.getMessage());
            }
        }
    }

    /**
     * Load browser configuration for pipeline execution.
     *
     * @param configPath Path relative to src/test/resources (e.g., "configuration/config_chrome_win10.json")
     * @return BrowserConfig with all settings
     */
    public BrowserConfig loadPipelineConfig(String configPath) {
        if (configPath == null || configPath.isEmpty()) {
            log.warn("No browser config path provided, returning defaults");
            return BrowserConfig.defaults();
        }

        // Check cache
        if (browserConfigCache.containsKey(configPath)) {
            return browserConfigCache.get(configPath);
        }

        File configFile = new File(CONFIG_BASE_PATH + configPath);
        if (!configFile.exists()) {
            log.warn("Browser config file not found: {}, returning defaults", configPath);
            return BrowserConfig.defaults();
        }

        try {
            JsonNode configNode = objectMapper.readTree(configFile);
            BrowserConfig config = parsePipelineConfig(configNode);
            browserConfigCache.put(configPath, config);
            log.info("Loaded browser config from {}: browser={}, platform={}",
                    configPath, config.getBrowserName(), config.getPlatformName());
            return config;
        } catch (IOException e) {
            log.error("Failed to load browser config from {}: {}", configPath, e.getMessage());
            return BrowserConfig.defaults();
        }
    }

    /**
     * Parse pipeline browser configuration from JSON.
     */
    private BrowserConfig parsePipelineConfig(JsonNode configNode) {
        BrowserConfig.BrowserConfigBuilder builder = BrowserConfig.builder();

        if (!configNode.has("browsers") || !configNode.get("browsers").isArray()
                || configNode.get("browsers").isEmpty()) {
            return BrowserConfig.defaults();
        }

        JsonNode browserNode = configNode.get("browsers").get(0);

        // Basic browser info
        builder.browserName(getTextValue(browserNode, "browserName", "chrome"));
        builder.platformName(getTextValue(browserNode, "platformName", "Windows 11"));
        builder.browserVersion(getTextValue(browserNode, "browserVersion", "latest"));

        // sauce:options
        if (browserNode.has("sauce:options")) {
            JsonNode sauceOptions = browserNode.get("sauce:options");
            Map<String, Object> sauceMap = new HashMap<>();

            sauceOptions.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                sauceMap.put(entry.getKey(), nodeToObject(value));
            });

            builder.sauceOptions(sauceMap);

            // Extract common sauce options
            builder.extendedDebugging(getBooleanValue(sauceOptions, "extendedDebugging", true));
            builder.screenResolution(getTextValue(sauceOptions, "screenResolution", "1920x1080"));
            builder.parentTunnel(getTextValue(sauceOptions, "parentTunnel", null));
            builder.tunnelIdentifier(getTextValue(sauceOptions, "tunnelIdentifier", null));
            builder.idleTimeout(getIntValue(sauceOptions, "idleTimeout", 300));
        }

        // goog:chromeOptions
        if (browserNode.has("goog:chromeOptions")) {
            JsonNode chromeOptions = browserNode.get("goog:chromeOptions");
            Map<String, Object> chromeMap = new HashMap<>();

            // Parse args
            if (chromeOptions.has("args") && chromeOptions.get("args").isArray()) {
                List<String> args = new ArrayList<>();
                chromeOptions.get("args").forEach(arg -> args.add(arg.asText()));
                chromeMap.put("args", args);
                builder.chromeArgs(args);
            }

            // Parse prefs
            if (chromeOptions.has("prefs")) {
                Map<String, Object> prefs = new HashMap<>();
                chromeOptions.get("prefs").fields().forEachRemaining(entry ->
                        prefs.put(entry.getKey(), nodeToObject(entry.getValue())));
                chromeMap.put("prefs", prefs);
                builder.chromePrefs(prefs);
            }

            builder.chromeOptions(chromeMap);
        }

        // moz:firefoxOptions
        if (browserNode.has("moz:firefoxOptions")) {
            JsonNode firefoxOptions = browserNode.get("moz:firefoxOptions");
            Map<String, Object> firefoxMap = new HashMap<>();
            firefoxOptions.fields().forEachRemaining(entry ->
                    firefoxMap.put(entry.getKey(), nodeToObject(entry.getValue())));
            builder.firefoxOptions(firefoxMap);
        }

        // ms:edgeOptions
        if (browserNode.has("ms:edgeOptions")) {
            JsonNode edgeOptions = browserNode.get("ms:edgeOptions");
            Map<String, Object> edgeMap = new HashMap<>();
            edgeOptions.fields().forEachRemaining(entry ->
                    edgeMap.put(entry.getKey(), nodeToObject(entry.getValue())));
            builder.edgeOptions(edgeMap);
        }

        return builder.build();
    }

    /**
     * Load local configuration from debug_config.json.
     *
     * @return BrowserConfig with local settings
     */
    public BrowserConfig loadLocalConfig() {
        if (debugConfig == null) {
            log.warn("debug_config.json not loaded, returning defaults");
            return BrowserConfig.defaults();
        }

        BrowserConfig.BrowserConfigBuilder builder = BrowserConfig.builder();

        // Basic browser info
        builder.browserName(getTextValue(debugConfig, "browser", "chrome"));
        builder.platformName("LOCAL");

        // Parse capabilities from "cap" section
        if (debugConfig.has("cap")) {
            JsonNode capNode = debugConfig.get("cap");
            List<String> chromeArgs = new ArrayList<>();
            Map<String, Object> chromePrefs = new HashMap<>();

            // Headless
            builder.headless(getBooleanValue(capNode, "headless", false));

            // Window size
            String windowSize = getTextValue(capNode, "window_size", "1920x1080");
            builder.screenResolution(windowSize);

            // Raw Chrome arguments from "chrome_args" array
            if (capNode.has("chrome_args") && capNode.get("chrome_args").isArray()) {
                capNode.get("chrome_args").forEach(arg -> chromeArgs.add(arg.asText()));
                log.debug("Added {} raw Chrome args from debug_config.json", chromeArgs.size());
            }

            // Chrome args based on boolean capabilities (only if not already in chrome_args)
            if (getBooleanValue(capNode, "disable_notifications", true)
                    && !chromeArgs.contains("--disable-notifications")) {
                chromeArgs.add("--disable-notifications");
            }
            if (getBooleanValue(capNode, "disable_popup_blocking", true)
                    && !chromeArgs.contains("--disable-popup-blocking")) {
                chromeArgs.add("--disable-popup-blocking");
            }
            if (getBooleanValue(capNode, "ignore_certificate_errors", true)
                    && !chromeArgs.contains("--ignore-certificate-errors")) {
                chromeArgs.add("--ignore-certificate-errors");
                if (!chromeArgs.contains("--ignore-ssl-errors=yes")) {
                    chromeArgs.add("--ignore-ssl-errors=yes");
                }
            }

            // Raw Chrome prefs from "chrome_prefs" object
            if (capNode.has("chrome_prefs") && capNode.get("chrome_prefs").isObject()) {
                capNode.get("chrome_prefs").fields().forEachRemaining(entry ->
                        chromePrefs.put(entry.getKey(), nodeToObject(entry.getValue())));
                log.debug("Added {} Chrome prefs from debug_config.json", chromePrefs.size());
            }

            // Chrome prefs for password manager (only if not already set)
            if (getBooleanValue(capNode, "disable_password_manager", true)) {
                chromePrefs.putIfAbsent("credentials_enable_service", false);
                chromePrefs.putIfAbsent("profile.password_manager_enabled", false);
                chromePrefs.putIfAbsent("profile.password_manager_leak_detection", false);
            }

            builder.chromeArgs(chromeArgs);
            builder.chromePrefs(chromePrefs);
        }

        log.info("Loaded local config from debug_config.json");
        return builder.build();
    }

    /**
     * Get text value from JSON node.
     */
    private String getTextValue(JsonNode node, String field, String defaultValue) {
        if (node != null && node.has(field)) {
            return node.get(field).asText();
        }
        return defaultValue;
    }

    /**
     * Get boolean value from JSON node.
     */
    private boolean getBooleanValue(JsonNode node, String field, boolean defaultValue) {
        if (node != null && node.has(field)) {
            return node.get(field).asBoolean();
        }
        return defaultValue;
    }

    /**
     * Get int value from JSON node.
     */
    private int getIntValue(JsonNode node, String field, int defaultValue) {
        if (node != null && node.has(field)) {
            return node.get(field).asInt();
        }
        return defaultValue;
    }

    /**
     * Convert JsonNode to Object.
     */
    private Object nodeToObject(JsonNode node) {
        if (node.isBoolean()) return node.asBoolean();
        if (node.isInt()) return node.asInt();
        if (node.isLong()) return node.asLong();
        if (node.isDouble()) return node.asDouble();
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(item -> list.add(nodeToObject(item)));
            return list;
        }
        if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            node.fields().forEachRemaining(entry ->
                    map.put(entry.getKey(), nodeToObject(entry.getValue())));
            return map;
        }
        return node.asText();
    }

    /**
     * Reset instance (for testing).
     */
    public static void reset() {
        instance = null;
    }

    /**
     * Browser configuration data class.
     */
    @Data
    @Builder
    public static class BrowserConfig {
        private String browserName;
        private String platformName;
        private String browserVersion;
        private boolean headless;

        // sauce:options
        private Map<String, Object> sauceOptions;
        private boolean extendedDebugging;
        private String screenResolution;
        private String parentTunnel;
        private String tunnelIdentifier;
        private int idleTimeout;

        // goog:chromeOptions
        private Map<String, Object> chromeOptions;
        private List<String> chromeArgs;
        private Map<String, Object> chromePrefs;

        // moz:firefoxOptions
        private Map<String, Object> firefoxOptions;

        // ms:edgeOptions
        private Map<String, Object> edgeOptions;

        /**
         * Create default configuration.
         */
        public static BrowserConfig defaults() {
            List<String> defaultArgs = new ArrayList<>();
            defaultArgs.add("--ignore-certificate-errors");
            defaultArgs.add("--disable-notifications");
            defaultArgs.add("--disable-popup-blocking");

            Map<String, Object> defaultPrefs = new HashMap<>();
            defaultPrefs.put("credentials_enable_service", false);
            defaultPrefs.put("profile.password_manager_enabled", false);

            return BrowserConfig.builder()
                    .browserName("chrome")
                    .platformName("Windows 11")
                    .browserVersion("latest")
                    .headless(false)
                    .extendedDebugging(true)
                    .screenResolution("1920x1080")
                    .idleTimeout(300)
                    .chromeArgs(defaultArgs)
                    .chromePrefs(defaultPrefs)
                    .build();
        }

        /**
         * Check if Chrome args are configured.
         */
        public boolean hasChromeArgs() {
            return chromeArgs != null && !chromeArgs.isEmpty();
        }

        /**
         * Check if Chrome prefs are configured.
         */
        public boolean hasChromePrefs() {
            return chromePrefs != null && !chromePrefs.isEmpty();
        }

        /**
         * Check if sauce options are configured.
         */
        public boolean hasSauceOptions() {
            return sauceOptions != null && !sauceOptions.isEmpty();
        }
    }
}
