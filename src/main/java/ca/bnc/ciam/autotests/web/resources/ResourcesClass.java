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
        this.properties = new Properties();
        loadProperties();
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
     *
     * @param key the property key
     * @return the property value, or the key itself if not found
     */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            log.warn("Property not found: {} in {}", key, buildFileName());
            return key; // Return key as fallback to make debugging easier
        }
        return value;
    }

    /**
     * Gets a property value by key with a default value.
     *
     * @param key          the property key
     * @param defaultValue the default value if key not found
     * @return the property value or default value
     */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * Checks if a property key exists.
     *
     * @param key the property key
     * @return true if key exists
     */
    public boolean containsKey(String key) {
        return properties.containsKey(key);
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
        return String.format("ResourcesClass[file=%s, language=%s, properties=%d]",
            resourceFileName, language, properties.size());
    }
}
