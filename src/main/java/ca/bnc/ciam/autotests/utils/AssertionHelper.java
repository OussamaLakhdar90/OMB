package ca.bnc.ciam.autotests.utils;

import ca.bnc.ciam.autotests.annotation.HasToBeIgnoredForAssertion;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for recursive field-by-field comparison of objects.
 */
@Slf4j
public final class AssertionHelper {

    private AssertionHelper() {
        // Utility class - prevent instantiation
    }

    /**
     * Compare two objects field by field recursively.
     * Fields marked with @HasToBeIgnoredForAssertion are excluded.
     *
     * @param expected The expected object
     * @param actual   The actual object
     * @param context  Description context for logging
     */
    public static void compareFieldByFieldRecursively(Object expected, Object actual, String context) {
        log.info("Comparing objects recursively: {}", context);

        List<String> fieldsToIgnore = getFieldsToIgnore(expected);
        log.debug("Fields to ignore: {}", fieldsToIgnore);

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields(fieldsToIgnore.toArray(new String[0]))
                .withStrictTypeChecking(false)
                .build();

        try {
            assertThat(actual)
                    .as(context)
                    .usingRecursiveComparison(config)
                    .isEqualTo(expected);
            log.info("PASS: Objects are equal - {}", context);
        } catch (AssertionError e) {
            log.error("FAIL: Objects differ - {}", context);
            log.error("Expected: {}", expected);
            log.error("Actual: {}", actual);
            throw e;
        }
    }

    /**
     * Compare two objects field by field recursively, ignoring specified fields.
     *
     * @param expected       The expected object
     * @param actual         The actual object
     * @param context        Description context for logging
     * @param fieldsToIgnore Additional fields to ignore
     */
    public static void compareFieldByFieldRecursively(Object expected, Object actual,
                                                      String context, String... fieldsToIgnore) {
        log.info("Comparing objects recursively: {}", context);

        List<String> allFieldsToIgnore = new ArrayList<>(getFieldsToIgnore(expected));
        allFieldsToIgnore.addAll(List.of(fieldsToIgnore));
        log.debug("Fields to ignore: {}", allFieldsToIgnore);

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields(allFieldsToIgnore.toArray(new String[0]))
                .withStrictTypeChecking(false)
                .build();

        try {
            assertThat(actual)
                    .as(context)
                    .usingRecursiveComparison(config)
                    .isEqualTo(expected);
            log.info("PASS: Objects are equal - {}", context);
        } catch (AssertionError e) {
            log.error("FAIL: Objects differ - {}", context);
            log.error("Expected: {}", expected);
            log.error("Actual: {}", actual);
            throw e;
        }
    }

    /**
     * Compare two objects allowing for specific tolerance in numeric fields.
     *
     * @param expected         The expected object
     * @param actual           The actual object
     * @param context          Description context for logging
     * @param doubleTolerance  Tolerance for double comparisons
     */
    public static void compareWithTolerance(Object expected, Object actual,
                                            String context, double doubleTolerance) {
        log.info("Comparing objects with tolerance: {} (tolerance: {})", context, doubleTolerance);

        List<String> fieldsToIgnore = getFieldsToIgnore(expected);

        RecursiveComparisonConfiguration config = RecursiveComparisonConfiguration.builder()
                .withIgnoredFields(fieldsToIgnore.toArray(new String[0]))
                .withStrictTypeChecking(false)
                .build();

        try {
            assertThat(actual)
                    .as(context)
                    .usingRecursiveComparison(config)
                    .withEqualsForType((d1, d2) -> Math.abs(d1 - d2) <= doubleTolerance, Double.class)
                    .withEqualsForType((f1, f2) -> Math.abs(f1 - f2) <= (float) doubleTolerance, Float.class)
                    .isEqualTo(expected);
            log.info("PASS: Objects are equal - {}", context);
        } catch (AssertionError e) {
            log.error("FAIL: Objects differ - {}", context);
            log.error("Expected: {}", expected);
            log.error("Actual: {}", actual);
            throw e;
        }
    }

    /**
     * Get list of field names marked with @HasToBeIgnoredForAssertion.
     */
    private static List<String> getFieldsToIgnore(Object object) {
        List<String> fieldsToIgnore = new ArrayList<>();
        if (object == null) {
            return fieldsToIgnore;
        }

        Class<?> clazz = object.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(HasToBeIgnoredForAssertion.class)) {
                    fieldsToIgnore.add(field.getName());
                }
            }
            clazz = clazz.getSuperclass();
        }

        return fieldsToIgnore;
    }

    /**
     * Clone an object for assertion preparation using reflection.
     *
     * @param source The source object to clone
     * @param <T>    The type of the object
     * @return A cloned instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T cloneForAssertion(T source) {
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
            log.error("Error cloning object for assertion", e);
            throw new RuntimeException("Failed to clone object for assertion", e);
        }
    }
}
