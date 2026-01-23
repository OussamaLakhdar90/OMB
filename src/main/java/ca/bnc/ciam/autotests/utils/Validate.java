package ca.bnc.ciam.autotests.utils;

import ca.bnc.ciam.autotests.web.elements.IElement;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebElement;

import java.util.Collection;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Custom validation utilities with logging support.
 * Provides fluent validation methods for common test assertions.
 */
@Slf4j
public final class Validate {

    private static final Object SYNC_LOCK = new Object();
    private static boolean failOnError = true;

    private Validate() {
        // Utility class - prevent instantiation
    }

    /**
     * Set whether validations should fail the test on error.
     */
    public static void setFailOnError(boolean fail) {
        failOnError = fail;
    }

    /**
     * Get whether validations fail the test on error.
     */
    public static boolean isFailOnError() {
        return failOnError;
    }

    /**
     * Alias for setFailOnError - controls whether assertions are called.
     * When false, methods return boolean results without throwing exceptions.
     */
    public static void setCallAssert(boolean callAssert) {
        failOnError = callAssert;
    }

    /**
     * String validation utilities.
     */
    public static class Strings {

        private Strings() {
        }

        /**
         * Validate that two strings are equal.
         */
        public static void areEqual(String expected, String actual, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating: {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .isEqualTo(expected);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that two strings are equal, ignoring case.
         */
        public static void areEqualIgnoreCase(String expected, String actual, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating (ignore case): {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .isEqualToIgnoringCase(expected);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a string contains a substring.
         */
        public static void contains(String actual, String substring, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating contains: {} - Actual: [{}], Should contain: [{}]", verificationContext, actual, substring);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .contains(substring);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - [{}] does not contain [{}]", verificationContext, actual, substring);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a string is not empty.
         */
        public static void isNotEmpty(String actual, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating not empty: {} - Actual: [{}]", verificationContext, actual);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .isNotEmpty();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - String is empty", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a string matches a regex pattern.
         */
        public static void matchesPattern(String actual, String pattern, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating pattern: {} - Actual: [{}], Pattern: [{}]", verificationContext, actual, pattern);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .matches(pattern);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - [{}] does not match pattern [{}]", verificationContext, actual, pattern);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Object validation utilities.
     */
    public static class Objects {

        private Objects() {
        }

        /**
         * Validate that an object is not null.
         */
        public static void isNotNull(Object object, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating not null: {}", verificationContext);
                try {
                    assertThat(object)
                            .as(verificationContext)
                            .isNotNull();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Object is null", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that an object is null.
         */
        public static void isNull(Object object, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating is null: {}", verificationContext);
                try {
                    assertThat(object)
                            .as(verificationContext)
                            .isNull();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Object is not null: {}", verificationContext, object);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that two objects are equal.
         */
        public static void areEqual(Object expected, Object actual, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating equal: {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                try {
                    assertThat(actual)
                            .as(verificationContext)
                            .isEqualTo(expected);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Expected: [{}], Actual: [{}]", verificationContext, expected, actual);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        // ==================== exists() methods ====================

        /**
         * Validate that a WebElement exists (is not null and is displayed).
         *
         * @param element the WebElement to check
         * @param verificationContext description for logging
         * @return true if element exists and is displayed, false otherwise
         */
        public static boolean exists(WebElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element exists: {}", verificationContext);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    boolean displayed = element.isDisplayed();
                    if (displayed) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Element is not displayed", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is not displayed");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that an IElement exists (is not null and is displayed).
         *
         * @param element the IElement to check
         * @param verificationContext description for logging
         * @return true if element exists and is displayed, false otherwise
         */
        public static boolean exists(IElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement exists: {}", verificationContext);
                try {
                    if (element == null || element.isNull()) {
                        log.error("FAIL: {} - IElement is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is null");
                        }
                        return false;
                    }
                    boolean displayed = element.isDisplayed();
                    if (displayed) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - IElement is not displayed", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is not displayed");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking IElement: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - IElement check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        // ==================== notExists() methods ====================

        /**
         * Validate that a WebElement does not exist (is null or not displayed).
         *
         * @param element the WebElement to check
         * @param verificationContext description for logging
         * @return true if element is null or not displayed, false otherwise
         */
        public static boolean notExists(WebElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element not exists: {}", verificationContext);
                try {
                    if (element == null) {
                        log.info("PASS: {} - Element is null", verificationContext);
                        return true;
                    }
                    boolean displayed = element.isDisplayed();
                    if (!displayed) {
                        log.info("PASS: {} - Element is not displayed", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Element exists and is displayed", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element exists and is displayed");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    // Element not found or stale - that's a pass for notExists
                    log.info("PASS: {} - Element not accessible: {}", verificationContext, e.getMessage());
                    return true;
                }
            }
        }

        /**
         * Validate that an IElement does not exist (is null or not displayed).
         *
         * @param element the IElement to check
         * @param verificationContext description for logging
         * @return true if element is null or not displayed, false otherwise
         */
        public static boolean notExists(IElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement not exists: {}", verificationContext);
                try {
                    if (element == null || element.isNull()) {
                        log.info("PASS: {} - IElement is null", verificationContext);
                        return true;
                    }
                    boolean displayed = element.isDisplayed();
                    if (!displayed) {
                        log.info("PASS: {} - IElement is not displayed", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - IElement exists and is displayed", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement exists and is displayed");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.info("PASS: {} - IElement not accessible: {}", verificationContext, e.getMessage());
                    return true;
                }
            }
        }

        // ==================== isEnabled() methods ====================

        /**
         * Validate that a WebElement is enabled.
         *
         * @param element the WebElement to check
         * @param verificationContext description for logging
         * @return true if element is enabled, false otherwise
         */
        public static boolean isEnabled(WebElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element is enabled: {}", verificationContext);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    boolean enabled = element.isEnabled();
                    if (enabled) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Element is not enabled", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is not enabled");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that an IElement is enabled.
         *
         * @param element the IElement to check
         * @param verificationContext description for logging
         * @return true if element is enabled, false otherwise
         */
        public static boolean isEnabled(IElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement is enabled: {}", verificationContext);
                try {
                    if (element == null || element.isNull()) {
                        log.error("FAIL: {} - IElement is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is null");
                        }
                        return false;
                    }
                    boolean enabled = element.isEnabled();
                    if (enabled) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - IElement is not enabled", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is not enabled");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking IElement: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - IElement check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        // ==================== isDisabled() methods ====================

        /**
         * Validate that a WebElement is disabled.
         *
         * @param element the WebElement to check
         * @param verificationContext description for logging
         * @return true if element is disabled, false otherwise
         */
        public static boolean isDisabled(WebElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element is disabled: {}", verificationContext);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    boolean enabled = element.isEnabled();
                    if (!enabled) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Element is enabled", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is enabled");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that an IElement is disabled.
         *
         * @param element the IElement to check
         * @param verificationContext description for logging
         * @return true if element is disabled, false otherwise
         */
        public static boolean isDisabled(IElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement is disabled: {}", verificationContext);
                try {
                    if (element == null || element.isNull()) {
                        log.error("FAIL: {} - IElement is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is null");
                        }
                        return false;
                    }
                    boolean enabled = element.isEnabled();
                    if (!enabled) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - IElement is enabled", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is enabled");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking IElement: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - IElement check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        // ==================== hasText() methods ====================

        /**
         * Validate that a WebElement has specific text.
         *
         * @param element the WebElement to check
         * @param expectedText the expected text
         * @param verificationContext description for logging
         * @return true if element text matches expected, false otherwise
         */
        public static boolean hasText(WebElement element, String expectedText, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element has text: {} - Expected: [{}]", verificationContext, expectedText);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    String actualText = element.getText();
                    if (expectedText.equals(actualText)) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Expected: [{}], Actual: [{}]", verificationContext, expectedText, actualText);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Text mismatch. Expected: [" + expectedText + "], Actual: [" + actualText + "]");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that an IElement has specific text.
         *
         * @param element the IElement to check
         * @param expectedText the expected text
         * @param verificationContext description for logging
         * @return true if element text matches expected, false otherwise
         */
        public static boolean hasText(IElement element, String expectedText, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement has text: {} - Expected: [{}]", verificationContext, expectedText);
                try {
                    if (element == null || element.isNull()) {
                        log.error("FAIL: {} - IElement is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is null");
                        }
                        return false;
                    }
                    String actualText = element.getText();
                    if (expectedText.equals(actualText)) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Expected: [{}], Actual: [{}]", verificationContext, expectedText, actualText);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Text mismatch. Expected: [" + expectedText + "], Actual: [" + actualText + "]");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking IElement: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - IElement check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        // ==================== containsText() methods ====================

        /**
         * Validate that a WebElement contains specific text.
         *
         * @param element the WebElement to check
         * @param substring the text to search for
         * @param verificationContext description for logging
         * @return true if element text contains substring, false otherwise
         */
        public static boolean containsText(WebElement element, String substring, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element contains text: {} - Substring: [{}]", verificationContext, substring);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    String actualText = element.getText();
                    if (actualText != null && actualText.contains(substring)) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Text [{}] does not contain [{}]", verificationContext, actualText, substring);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Text [" + actualText + "] does not contain [" + substring + "]");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that an IElement contains specific text.
         *
         * @param element the IElement to check
         * @param substring the text to search for
         * @param verificationContext description for logging
         * @return true if element text contains substring, false otherwise
         */
        public static boolean containsText(IElement element, String substring, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating IElement contains text: {} - Substring: [{}]", verificationContext, substring);
                try {
                    if (element == null || element.isNull()) {
                        log.error("FAIL: {} - IElement is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - IElement is null");
                        }
                        return false;
                    }
                    String actualText = element.getText();
                    if (actualText != null && actualText.contains(substring)) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Text [{}] does not contain [{}]", verificationContext, actualText, substring);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Text [" + actualText + "] does not contain [" + substring + "]");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking IElement: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - IElement check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        // ==================== hasAttribute() methods ====================

        /**
         * Validate that a WebElement has a specific attribute value.
         *
         * @param element the WebElement to check
         * @param attributeName the attribute name
         * @param expectedValue the expected attribute value
         * @param verificationContext description for logging
         * @return true if attribute matches expected value, false otherwise
         */
        public static boolean hasAttribute(WebElement element, String attributeName, String expectedValue, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element has attribute: {} - Attribute: [{}], Expected: [{}]", verificationContext, attributeName, expectedValue);
                try {
                    if (element == null) {
                        log.error("FAIL: {} - Element is null", verificationContext);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Element is null");
                        }
                        return false;
                    }
                    String actualValue = element.getAttribute(attributeName);
                    if (expectedValue.equals(actualValue)) {
                        log.info("PASS: {}", verificationContext);
                        return true;
                    } else {
                        log.error("FAIL: {} - Attribute [{}]: Expected: [{}], Actual: [{}]", verificationContext, attributeName, expectedValue, actualValue);
                        if (failOnError) {
                            throw new AssertionError(verificationContext + " - Attribute [" + attributeName + "] mismatch. Expected: [" + expectedValue + "], Actual: [" + actualValue + "]");
                        }
                        return false;
                    }
                } catch (AssertionError e) {
                    throw e;
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
                    return false;
                }
            }
        }

        /**
         * Validate that a WebElement exists with specified wait time.
         */
        public static void existsWaitSeconds(WebElement element, int waitSeconds, String verificationContext) {
            // For now, delegate to exists - wait logic can be added if needed
            exists(element, verificationContext);
        }
    }

    /**
     * Boolean validation utilities.
     */
    public static class Booleans {

        private Booleans() {
        }

        /**
         * Validate that a condition is true.
         */
        public static void isTrue(boolean condition, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating is true: {}", verificationContext);
                try {
                    assertThat(condition)
                            .as(verificationContext)
                            .isTrue();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Condition is false", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a condition is false.
         */
        public static void isFalse(boolean condition, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating is false: {}", verificationContext);
                try {
                    assertThat(condition)
                            .as(verificationContext)
                            .isFalse();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Condition is true", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Collection validation utilities.
     */
    public static class Collections {

        private Collections() {
        }

        /**
         * Validate that a collection is not empty.
         */
        public static void isNotEmpty(Collection<?> collection, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating collection not empty: {}", verificationContext);
                try {
                    assertThat(collection)
                            .as(verificationContext)
                            .isNotEmpty();
                    log.info("PASS: {} - Size: {}", verificationContext, collection.size());
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Collection is empty", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a collection has a specific size.
         */
        public static void hasSize(Collection<?> collection, int expectedSize, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating collection size: {} - Expected: {}, Actual: {}",
                        verificationContext, expectedSize, collection != null ? collection.size() : "null");
                try {
                    assertThat(collection)
                            .as(verificationContext)
                            .hasSize(expectedSize);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Expected size: {}, Actual size: {}",
                            verificationContext, expectedSize, collection != null ? collection.size() : "null");
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a collection contains an element.
         */
        public static <T> void contains(Collection<T> collection, T element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating collection contains: {} - Element: {}", verificationContext, element);
                try {
                    assertThat(collection)
                            .as(verificationContext)
                            .contains(element);
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Collection does not contain: {}", verificationContext, element);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }
    }

    /**
     * Number validation utilities.
     */
    public static class Numbers {

        private Numbers() {
        }

        /**
         * Validate that a number is greater than another.
         */
        public static void isGreaterThan(Number actual, Number expected, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating greater than: {} - Actual: {}, Expected: >{}", verificationContext, actual, expected);
                try {
                    assertThat(actual.doubleValue())
                            .as(verificationContext)
                            .isGreaterThan(expected.doubleValue());
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - {} is not greater than {}", verificationContext, actual, expected);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a number is less than another.
         */
        public static void isLessThan(Number actual, Number expected, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating less than: {} - Actual: {}, Expected: <{}", verificationContext, actual, expected);
                try {
                    assertThat(actual.doubleValue())
                            .as(verificationContext)
                            .isLessThan(expected.doubleValue());
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - {} is not less than {}", verificationContext, actual, expected);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }

        /**
         * Validate that a number is between two values (inclusive).
         */
        public static void isBetween(Number actual, Number low, Number high, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating between: {} - Actual: {}, Range: [{}, {}]", verificationContext, actual, low, high);
                try {
                    assertThat(actual.doubleValue())
                            .as(verificationContext)
                            .isBetween(low.doubleValue(), high.doubleValue());
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - {} is not between {} and {}", verificationContext, actual, low, high);
                    if (failOnError) {
                        throw e;
                    }
                }
            }
        }
    }
}
