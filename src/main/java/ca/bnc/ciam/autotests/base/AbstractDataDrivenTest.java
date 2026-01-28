package ca.bnc.ciam.autotests.base;

import ca.bnc.ciam.autotests.data.TestData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for data-driven tests.
 * Provides world context for sharing data between test steps.
 *
 * Dependency checking is handled automatically by TestngListener.
 * Just annotate test methods with @DependentStep to skip them if previous tests fail.
 */
@Slf4j
public abstract class AbstractDataDrivenTest {

    /**
     * Keys for the world context.
     */
    public enum WorldKey {
        RAW_RESPONSE,
        RESPONSE_BODY,
        RESPONSE_STATUS,
        REQUEST_BODY,
        EXPECTED_OBJECT,
        ACTUAL_OBJECT,
        AUTH_TOKEN,
        SESSION_ID,
        USER_DATA,
        CUSTOM_DATA
    }

    /**
     * Thread-local storage for test data sharing between steps.
     */
    private static final ThreadLocal<Map<WorldKey, Object>> worldLocal = ThreadLocal.withInitial(HashMap::new);

    /**
     * Thread-local storage for test data map.
     */
    private static final ThreadLocal<Map<String, String>> testDataLocal = ThreadLocal.withInitial(HashMap::new);

    /**
     * Instance-level storage for test data (survives thread migration in @Factory pattern).
     * When using @Factory with parallel="classes", the constructor runs on a different thread
     * than the test methods. This instance field preserves the data across threads.
     */
    private Map<String, String> instanceTestData;

    /**
     * Shared context across all threads (for cross-test data sharing).
     */
    private static final Map<String, Object> sharedContext = new ConcurrentHashMap<>();

    /**
     * Store a value in the world context.
     *
     * @param key   The world key
     * @param value The value to store
     */
    protected static void pushToTheWorld(WorldKey key, Object value) {
        worldLocal.get().put(key, value);
        log.debug("Pushed to world: {} = {}", key, value != null ? value.getClass().getSimpleName() : "null");
    }

    /**
     * Retrieve a value from the world context.
     *
     * @param key        The world key
     * @param valueClass The expected value class
     * @param <T>        The value type
     * @return The value, or null if not found
     */
    @SuppressWarnings("unchecked")
    protected static <T> T pullFromTheWorld(WorldKey key, Class<T> valueClass) {
        Object value = worldLocal.get().get(key);
        if (value == null) {
            log.debug("World key {} not found", key);
            return null;
        }
        if (!valueClass.isInstance(value)) {
            log.warn("World key {} has unexpected type. Expected: {}, Actual: {}",
                    key, valueClass.getSimpleName(), value.getClass().getSimpleName());
            return null;
        }
        return (T) value;
    }

    /**
     * Check if a key exists in the world context.
     *
     * @param key The world key
     * @return True if the key exists
     */
    protected static boolean worldHas(WorldKey key) {
        return worldLocal.get().containsKey(key);
    }

    /**
     * Remove a value from the world context.
     *
     * @param key The world key
     */
    protected static void removeFromWorld(WorldKey key) {
        worldLocal.get().remove(key);
        log.debug("Removed from world: {}", key);
    }

    /**
     * Clear the world context for the current thread.
     */
    protected static void clearWorldLocal() {
        worldLocal.get().clear();
        log.debug("World context cleared");
    }

    /**
     * Set the test data map for the current test.
     * Stores data both in ThreadLocal (for current thread access) and
     * instance field (for thread migration support in @Factory pattern).
     *
     * @param data The test data map
     */
    protected void setTestData(Map<String, String> data) {
        this.instanceTestData = data;
        testDataLocal.set(data);
        log.debug("Test data set: {} entries", data != null ? data.size() : 0);
    }

    /**
     * Get the test data map for the current test.
     * If ThreadLocal is empty but instance field has data (thread migration scenario),
     * automatically syncs the instance data to the current thread's ThreadLocal.
     *
     * @return The test data map
     */
    protected Map<String, String> getTestData() {
        Map<String, String> threadData = testDataLocal.get();

        // Handle thread migration: if ThreadLocal is empty but instance has data, sync it
        if ((threadData == null || threadData.isEmpty()) && instanceTestData != null && !instanceTestData.isEmpty()) {
            log.debug("Thread migration detected - syncing instance data to ThreadLocal ({} entries)", instanceTestData.size());
            testDataLocal.set(instanceTestData);
            return instanceTestData;
        }

        return threadData;
    }

    /**
     * Create a TestData instance with the current test data.
     * Uses getTestData() to ensure thread migration is handled properly.
     *
     * @return A new TestData instance
     */
    protected TestData testData() {
        return new TestData(getTestData());
    }

    /**
     * Store a value in the shared context (accessible across tests/threads).
     *
     * @param key   The context key
     * @param value The value to store
     */
    protected static void setSharedContext(String key, Object value) {
        sharedContext.put(key, value);
        log.debug("Set shared context: {} = {}", key, value != null ? value.getClass().getSimpleName() : "null");
    }

    /**
     * Retrieve a value from the shared context.
     *
     * @param key        The context key
     * @param valueClass The expected value class
     * @param <T>        The value type
     * @return The value, or null if not found
     */
    @SuppressWarnings("unchecked")
    protected static <T> T getSharedContext(String key, Class<T> valueClass) {
        Object value = sharedContext.get(key);
        if (value == null) {
            return null;
        }
        if (!valueClass.isInstance(value)) {
            log.warn("Shared context key {} has unexpected type. Expected: {}, Actual: {}",
                    key, valueClass.getSimpleName(), value.getClass().getSimpleName());
            return null;
        }
        return (T) value;
    }

    /**
     * Clear the shared context.
     */
    protected static void clearSharedContext() {
        sharedContext.clear();
        log.debug("Shared context cleared");
    }

    /**
     * Clean up all thread-local storage.
     * Should be called at the end of each test class/suite.
     */
    public static void cleanUp() {
        worldLocal.remove();
        testDataLocal.remove();
        log.debug("Thread-local storage cleaned up");
    }
}
