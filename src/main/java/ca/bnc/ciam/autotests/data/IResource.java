package ca.bnc.ciam.autotests.data;

/**
 * Interface for resource access.
 */
public interface IResource {

    /**
     * Get a resource value by key.
     *
     * @param key The resource key
     * @return The resource value
     */
    String get(String key);

    /**
     * Get a resource value by key with default.
     *
     * @param key          The resource key
     * @param defaultValue The default value if key not found
     * @return The resource value or default
     */
    String get(String key, String defaultValue);
}
