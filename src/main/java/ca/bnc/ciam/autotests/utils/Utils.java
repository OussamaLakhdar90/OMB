package ca.bnc.ciam.autotests.utils;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * General utility methods for the framework.
 */
@Slf4j
public final class Utils {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Utils() {
        // Utility class - prevent instantiation
    }

    /**
     * Generate a unique ID for test runs.
     */
    public static String generateRunId() {
        return "run-" + LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    /**
     * Generate a UUID string.
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Get current timestamp as formatted string.
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMATTER);
    }

    /**
     * Get current datetime as formatted string.
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    /**
     * Get current datetime as ISO string.
     */
    public static String getCurrentDateTimeISO() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Sleep for specified milliseconds.
     */
    public static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }

    /**
     * Sleep for specified seconds.
     */
    public static void sleepSeconds(int seconds) {
        sleep(seconds * 1000L);
    }

    /**
     * Clone an object using reflection (shallow copy).
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneObject(T source) {
        if (source == null) {
            return null;
        }

        try {
            Class<?> clazz = source.getClass();
            T target = (T) clazz.getDeclaredConstructor().newInstance();

            while (clazz != null && clazz != Object.class) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(source);
                    field.set(target, value);
                }
                clazz = clazz.getSuperclass();
            }

            return target;
        } catch (Exception e) {
            log.error("Error cloning object", e);
            throw new RuntimeException("Failed to clone object", e);
        }
    }

    /**
     * Check if a string is null or empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Check if a string is not null and not empty.
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Get string value or default if null/empty.
     */
    public static String getOrDefault(String value, String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    /**
     * Sanitize a string for use in file names.
     */
    public static String sanitizeForFileName(String input) {
        if (input == null) {
            return "unknown";
        }
        return input.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Convert a test method name to a readable description.
     * e.g., "t001_Navigate_And_Switch_Language" -> "Navigate And Switch Language"
     */
    public static String methodNameToDescription(String methodName) {
        if (methodName == null) {
            return "";
        }
        // Remove prefix like t001_, t002_, etc.
        String withoutPrefix = methodName.replaceFirst("^t\\d+_", "");
        // Replace underscores with spaces
        return withoutPrefix.replace("_", " ");
    }

    /**
     * Get the step number from a test method name.
     * e.g., "t001_Navigate_And_Switch_Language" -> 1
     */
    public static int getStepNumber(String methodName) {
        if (methodName == null) {
            return 0;
        }
        try {
            String prefix = methodName.split("_")[0];
            if (prefix.startsWith("t")) {
                return Integer.parseInt(prefix.substring(1));
            }
        } catch (Exception e) {
            log.debug("Could not extract step number from: {}", methodName);
        }
        return 0;
    }

    /**
     * Format duration in milliseconds to human-readable string.
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + " ms";
        } else if (millis < 60000) {
            return String.format("%.2f sec", millis / 1000.0);
        } else if (millis < 3600000) {
            long minutes = millis / 60000;
            long seconds = (millis % 60000) / 1000;
            return String.format("%d min %d sec", minutes, seconds);
        } else {
            long hours = millis / 3600000;
            long minutes = (millis % 3600000) / 60000;
            return String.format("%d hr %d min", hours, minutes);
        }
    }

    /**
     * Get environment variable with fallback to system property.
     */
    public static String getEnvOrProperty(String key, String defaultValue) {
        String value = System.getenv(key);
        if (isEmpty(value)) {
            value = System.getProperty(key);
        }
        return isEmpty(value) ? defaultValue : value;
    }

    /**
     * Get environment variable with fallback to system property (no default).
     */
    public static String getEnvOrProperty(String key) {
        return getEnvOrProperty(key, null);
    }
}
