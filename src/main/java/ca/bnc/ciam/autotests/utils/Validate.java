package ca.bnc.ciam.autotests.utils;

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

        /**
         * Validate that a WebElement exists (is not null and is displayed).
         */
        public static void exists(WebElement element, String verificationContext) {
            synchronized (SYNC_LOCK) {
                log.info("Validating element exists: {}", verificationContext);
                try {
                    assertThat(element)
                            .as(verificationContext + " - element not null")
                            .isNotNull();
                    assertThat(element.isDisplayed())
                            .as(verificationContext + " - element is displayed")
                            .isTrue();
                    log.info("PASS: {}", verificationContext);
                } catch (AssertionError e) {
                    log.error("FAIL: {} - Element does not exist or is not displayed", verificationContext);
                    if (failOnError) {
                        throw e;
                    }
                } catch (Exception e) {
                    log.error("FAIL: {} - Error checking element: {}", verificationContext, e.getMessage());
                    if (failOnError) {
                        throw new AssertionError(verificationContext + " - Element check failed: " + e.getMessage(), e);
                    }
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
