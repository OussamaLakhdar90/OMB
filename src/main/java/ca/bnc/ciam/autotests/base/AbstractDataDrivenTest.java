package ca.bnc.ciam.autotests.base;

import ca.bnc.ciam.autotests.data.TestData;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for data-driven tests.
 * Provides world context for sharing data between test steps.
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
     * Thread-local storage for continuation flag.
     */
    private static final ThreadLocal<Boolean> continueNextStep = ThreadLocal.withInitial(() -> true);

    /**
     * Thread-local storage for test failure flag.
     */
    private static final ThreadLocal<Boolean> testFailed = ThreadLocal.withInitial(() -> false);

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
     *
     * @param data The test data map
     */
    protected static void setTestData(Map<String, String> data) {
        testDataLocal.set(data);
    }

    /**
     * Get the test data map for the current test.
     *
     * @return The test data map
     */
    protected static Map<String, String> getTestData() {
        return testDataLocal.get();
    }

    /**
     * Create a TestData instance with the current test data.
     *
     * @return A new TestData instance
     */
    protected TestData testData() {
        return new TestData(testDataLocal.get());
    }

    /**
     * Start continuation check for dependent steps.
     */
    protected void startContinuationCheck() {
        continueNextStep.set(true);
        testFailed.set(false);
        log.debug("Continuation check started");
    }

    /**
     * Stop continuation (mark test as failed).
     */
    protected static void stopContinuation() {
        continueNextStep.set(false);
        testFailed.set(true);
        log.debug("Continuation stopped - test marked as failed");
    }

    /**
     * Check if continuation should proceed.
     *
     * @return True if test should continue
     */
    protected static boolean shouldContinue() {
        return continueNextStep.get();
    }

    /**
     * Check if the test has failed.
     *
     * @return True if test has failed
     */
    protected static boolean hasTestFailed() {
        return testFailed.get();
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
        continueNextStep.remove();
        testFailed.remove();
        log.debug("Thread-local storage cleaned up");
    }
}
