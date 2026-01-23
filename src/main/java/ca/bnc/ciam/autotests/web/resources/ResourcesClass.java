package ca.bnc.ciam.autotests.web.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * ResourcesClass for loading and accessing page-specific resources.
 * Loads properties files based on page name and language, following the pattern:
 * {pageName}_{language}.properties (e.g., login_page_en.properties, login_page_fr.properties)
 *
 * Resource files should be placed in:
 * - src/main/resources/pages/{pageName}_{language}.properties
 * - src/main/resources/{pageName}_{language}.properties
 *
 * Property keys should be prefixed with the page class name (camelCase):
 * loginPage.username.label=Username
 * loginPage.password.label=Password
 *
 * Usage in Page Object:
 * <pre>
 * public class LoginPage extends PageObject {
 *     private final ResourcesClass resource;
 *
 *     public LoginPage(WebDriver driver, String language) {
 *         super(driver);
 *         resource = new ResourcesClass("login_page", language, this);
 *     }
 *
 *     public String getUsernameLabel() {
 *         // Looks up "loginPage.username.label" automatically
 *         return resource.get("username.label");
 *     }
 * }
 * </pre>
 */
@Slf4j
public class ResourcesClass {

    @Getter
    private final Properties properties;

    @Getter
    private final String resourceFileName;

    @Getter
    private final String language;

    @Getter
    private final String prefix;

    private final Object pageObject;

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String PROPERTIES_EXTENSION = ".properties";

    /**
     * Creates a ResourcesClass with the default language (English).
     *
     * @param resourceFileName the base name of the properties file (without language suffix)
     * @param obj              the page object (used to get class loader context)
     */
    public ResourcesClass(String resourceFileName, Object obj) {
        this(resourceFileName, DEFAULT_LANGUAGE, obj);
    }

    /**
     * Creates a ResourcesClass with a specific language.
     *
     * @param resourceFileName the base name of the properties file
     * @param language         the language code (e.g., "en", "fr")
     * @param obj              the page object
     */
    public ResourcesClass(String resourceFileName, String language, Object obj) {
        this.resourceFileName = resourceFileName;
        this.language = language != null && !language.isEmpty() ? language : DEFAULT_LANGUAGE;
        this.pageObject = obj;
        this.prefix = derivePrefix(obj);
        this.properties = new Properties();
        loadProperties();
    }

    /**
     * Derives the property key prefix from the page object's class name.
     * Converts class name to camelCase (e.g., LoginPage -> loginPage).
     *
     * @param obj the page object
     * @return the prefix string
     */
    private String derivePrefix(Object obj) {
        if (obj == null) {
            return "";
        }
        String className = obj.getClass().getSimpleName();
        if (className.isEmpty()) {
            return "";
        }
        // Convert first letter to lowercase (LoginPage -> loginPage)
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * Loads properties from the resource file.
     */
    private void loadProperties() {
        String fileName = buildFileName();

        // Try to load from classpath
        try (InputStream inputStream = getInputStream(fileName)) {
            if (inputStream != null) {
                properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                log.debug("Loaded resources from: {}", fileName);
            } else {
                log.warn("Resource file not found: {}", fileName);
                // Try fallback to default language if different
                if (!language.equals(DEFAULT_LANGUAGE)) {
                    String fallbackFileName = resourceFileName + "_" + DEFAULT_LANGUAGE + PROPERTIES_EXTENSION;
                    try (InputStream fallbackStream = getInputStream(fallbackFileName)) {
                        if (fallbackStream != null) {
                            properties.load(new InputStreamReader(fallbackStream, StandardCharsets.UTF_8));
                            log.info("Loaded fallback resources from: {} (language '{}' not found)",
                                    fallbackFileName, language);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error loading resource file {}: {}", fileName, e.getMessage());
        }
    }

    /**
     * Builds the full file name with language suffix.
     */
    private String buildFileName() {
        return resourceFileName + "_" + language + PROPERTIES_EXTENSION;
    }

    /**
     * Gets an input stream for the given file name.
     */
    private InputStream getInputStream(String fileName) {
        ClassLoader classLoader = pageObject != null
            ? pageObject.getClass().getClassLoader()
            : Thread.currentThread().getContextClassLoader();

        // Try direct path
        InputStream stream = classLoader.getResourceAsStream(fileName);
        if (stream != null) {
            return stream;
        }

        // Try with pages/ prefix
        stream = classLoader.getResourceAsStream("pages/" + fileName);
        if (stream != null) {
            return stream;
        }

        // Try with resources/ prefix
        stream = classLoader.getResourceAsStream("resources/" + fileName);
        return stream;
    }

    /**
     * Gets a property value by key.
     * Automatically prepends the page prefix to the key.
     * Example: For LoginPage, get("username.label") looks up "loginPage.username.label"
     *
     * @param key the property key (without prefix)
     * @return the property value, or the full key if not found
     */
    public String get(String key) {
        String fullKey = buildFullKey(key);
        String value = properties.getProperty(fullKey);
        if (value == null) {
            // Try without prefix as fallback
            value = properties.getProperty(key);
            if (value == null) {
                log.warn("Property not found: {} (tried {} and {}) in {}",
                        key, fullKey, key, buildFileName());
                return fullKey; // Return full key as fallback to make debugging easier
            }
        }
        return value;
    }

    /**
     * Gets a property value by key with a default value.
     * Automatically prepends the page prefix to the key.
     *
     * @param key          the property key (without prefix)
     * @param defaultValue the default value if key not found
     * @return the property value or default value
     */
    public String get(String key, String defaultValue) {
        String fullKey = buildFullKey(key);
        String value = properties.getProperty(fullKey);
        if (value == null) {
            // Try without prefix as fallback
            value = properties.getProperty(key, defaultValue);
        }
        return value;
    }

    /**
     * Gets a property value by full key (without adding prefix).
     * Use this when you want to access a key that doesn't follow the prefix pattern.
     *
     * @param fullKey the complete property key
     * @return the property value, or the key itself if not found
     */
    public String getByFullKey(String fullKey) {
        String value = properties.getProperty(fullKey);
        if (value == null) {
            log.warn("Property not found: {} in {}", fullKey, buildFileName());
            return fullKey;
        }
        return value;
    }

    /**
     * Builds the full property key by prepending the prefix.
     *
     * @param key the key without prefix
     * @return the full key with prefix (e.g., "loginPage.username.label")
     */
    private String buildFullKey(String key) {
        if (prefix == null || prefix.isEmpty()) {
            return key;
        }
        return prefix + "." + key;
    }

    /**
     * Checks if a property key exists.
     * Checks both with and without the page prefix.
     *
     * @param key the property key (without prefix)
     * @return true if key exists
     */
    public boolean containsKey(String key) {
        String fullKey = buildFullKey(key);
        return properties.containsKey(fullKey) || properties.containsKey(key);
    }

    /**
     * Gets a formatted property value.
     *
     * @param key  the property key
     * @param args the format arguments
     * @return the formatted property value
     */
    public String getFormatted(String key, Object... args) {
        String pattern = get(key);
        try {
            return String.format(pattern, args);
        } catch (Exception e) {
            log.warn("Error formatting property {}: {}", key, e.getMessage());
            return pattern;
        }
    }

    /**
     * Gets all properties.
     *
     * @return the Properties object
     */
    public Properties getResource() {
        return properties;
    }

    /**
     * Gets the number of loaded properties.
     *
     * @return the count of properties
     */
    public int size() {
        return properties.size();
    }

    /**
     * Checks if any properties were loaded.
     *
     * @return true if properties exist
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Reloads properties with a new language.
     *
     * @param newLanguage the new language code
     * @return a new ResourcesClass with the specified language
     */
    public ResourcesClass withLanguage(String newLanguage) {
        return new ResourcesClass(resourceFileName, newLanguage, pageObject);
    }

    @Override
    public String toString() {
        return String.format("ResourcesClass[file=%s, language=%s, prefix=%s, properties=%d]",
            resourceFileName, language, prefix, properties.size());
    }
}
