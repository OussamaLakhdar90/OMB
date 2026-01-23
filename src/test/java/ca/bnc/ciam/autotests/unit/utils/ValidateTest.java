package ca.bnc.ciam.autotests.unit.utils;

import ca.bnc.ciam.autotests.utils.Validate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for Validate class.
 * Tests validation utilities for strings, objects, booleans, collections, and numbers.
 */
@Test(groups = "unit")
public class ValidateTest {

    @BeforeMethod
    public void setUp() {
        // Ensure fail on error is enabled for most tests
        Validate.setFailOnError(true);
    }

    @AfterMethod
    public void tearDown() {
        // Reset to default
        Validate.setFailOnError(true);
    }

    // ===========================================
    // Static Configuration Tests
    // ===========================================

    @Test
    public void testSetFailOnError_True() {
        Validate.setFailOnError(true);
        assertThat(Validate.isFailOnError()).isTrue();
    }

    @Test
    public void testSetFailOnError_False() {
        Validate.setFailOnError(false);
        assertThat(Validate.isFailOnError()).isFalse();
    }

    // ===========================================
    // Strings.areEqual Tests
    // ===========================================

    @Test
    public void testStrings_AreEqual_SameStrings_Passes() {
        assertThatCode(() -> Validate.Strings.areEqual("test", "test", "String equality"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_AreEqual_DifferentStrings_Fails() {
        assertThatThrownBy(() -> Validate.Strings.areEqual("expected", "actual", "String mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testStrings_AreEqual_FailOnErrorFalse_DoesNotThrow() {
        Validate.setFailOnError(false);
        assertThatCode(() -> Validate.Strings.areEqual("expected", "actual", "Soft assertion"))
                .doesNotThrowAnyException();
    }

    // ===========================================
    // Strings.areEqualIgnoreCase Tests
    // ===========================================

    @Test
    public void testStrings_AreEqualIgnoreCase_SameCase_Passes() {
        assertThatCode(() -> Validate.Strings.areEqualIgnoreCase("TEST", "TEST", "Case match"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_AreEqualIgnoreCase_DifferentCase_Passes() {
        assertThatCode(() -> Validate.Strings.areEqualIgnoreCase("TEST", "test", "Case ignore"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_AreEqualIgnoreCase_Different_Fails() {
        assertThatThrownBy(() -> Validate.Strings.areEqualIgnoreCase("hello", "world", "Case mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Strings.contains Tests
    // ===========================================

    @Test
    public void testStrings_Contains_SubstringExists_Passes() {
        assertThatCode(() -> Validate.Strings.contains("hello world", "world", "Contains substring"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_Contains_SubstringMissing_Fails() {
        assertThatThrownBy(() -> Validate.Strings.contains("hello world", "foo", "Missing substring"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Strings.isNotEmpty Tests
    // ===========================================

    @Test
    public void testStrings_IsNotEmpty_NonEmpty_Passes() {
        assertThatCode(() -> Validate.Strings.isNotEmpty("text", "Not empty"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_IsNotEmpty_Empty_Fails() {
        assertThatThrownBy(() -> Validate.Strings.isNotEmpty("", "Empty string"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Strings.matchesPattern Tests
    // ===========================================

    @Test
    public void testStrings_MatchesPattern_Valid_Passes() {
        assertThatCode(() -> Validate.Strings.matchesPattern("test123", "\\w+\\d+", "Pattern match"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testStrings_MatchesPattern_Invalid_Fails() {
        assertThatThrownBy(() -> Validate.Strings.matchesPattern("test", "\\d+", "Pattern mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Objects.isNotNull Tests
    // ===========================================

    @Test
    public void testObjects_IsNotNull_NonNull_Passes() {
        assertThatCode(() -> Validate.Objects.isNotNull("object", "Not null"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testObjects_IsNotNull_Null_Fails() {
        assertThatThrownBy(() -> Validate.Objects.isNotNull(null, "Null object"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Objects.isNull Tests
    // ===========================================

    @Test
    public void testObjects_IsNull_Null_Passes() {
        assertThatCode(() -> Validate.Objects.isNull(null, "Is null"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testObjects_IsNull_NonNull_Fails() {
        assertThatThrownBy(() -> Validate.Objects.isNull("object", "Not null"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Objects.areEqual Tests
    // ===========================================

    @Test
    public void testObjects_AreEqual_SameObjects_Passes() {
        assertThatCode(() -> Validate.Objects.areEqual(42, 42, "Equal objects"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testObjects_AreEqual_DifferentObjects_Fails() {
        assertThatThrownBy(() -> Validate.Objects.areEqual(1, 2, "Unequal objects"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Booleans.isTrue Tests
    // ===========================================

    @Test
    public void testBooleans_IsTrue_True_Passes() {
        assertThatCode(() -> Validate.Booleans.isTrue(true, "Is true"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testBooleans_IsTrue_False_Fails() {
        assertThatThrownBy(() -> Validate.Booleans.isTrue(false, "Is false"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Booleans.isFalse Tests
    // ===========================================

    @Test
    public void testBooleans_IsFalse_False_Passes() {
        assertThatCode(() -> Validate.Booleans.isFalse(false, "Is false"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testBooleans_IsFalse_True_Fails() {
        assertThatThrownBy(() -> Validate.Booleans.isFalse(true, "Is true"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Collections.isNotEmpty Tests
    // ===========================================

    @Test
    public void testCollections_IsNotEmpty_NonEmpty_Passes() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThatCode(() -> Validate.Collections.isNotEmpty(list, "Not empty"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCollections_IsNotEmpty_Empty_Fails() {
        List<String> list = Collections.emptyList();
        assertThatThrownBy(() -> Validate.Collections.isNotEmpty(list, "Empty list"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Collections.hasSize Tests
    // ===========================================

    @Test
    public void testCollections_HasSize_CorrectSize_Passes() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThatCode(() -> Validate.Collections.hasSize(list, 3, "Size check"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCollections_HasSize_WrongSize_Fails() {
        List<String> list = Arrays.asList("a", "b");
        assertThatThrownBy(() -> Validate.Collections.hasSize(list, 3, "Size mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Collections.contains Tests
    // ===========================================

    @Test
    public void testCollections_Contains_ElementExists_Passes() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThatCode(() -> Validate.Collections.contains(list, "b", "Contains element"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCollections_Contains_ElementMissing_Fails() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertThatThrownBy(() -> Validate.Collections.contains(list, "x", "Missing element"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Numbers.isGreaterThan Tests
    // ===========================================

    @Test
    public void testNumbers_IsGreaterThan_True_Passes() {
        assertThatCode(() -> Validate.Numbers.isGreaterThan(10, 5, "Greater than"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNumbers_IsGreaterThan_False_Fails() {
        assertThatThrownBy(() -> Validate.Numbers.isGreaterThan(3, 5, "Not greater"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testNumbers_IsGreaterThan_Equal_Fails() {
        assertThatThrownBy(() -> Validate.Numbers.isGreaterThan(5, 5, "Equal values"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Numbers.isLessThan Tests
    // ===========================================

    @Test
    public void testNumbers_IsLessThan_True_Passes() {
        assertThatCode(() -> Validate.Numbers.isLessThan(3, 5, "Less than"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNumbers_IsLessThan_False_Fails() {
        assertThatThrownBy(() -> Validate.Numbers.isLessThan(10, 5, "Not less"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Numbers.isBetween Tests
    // ===========================================

    @Test
    public void testNumbers_IsBetween_InRange_Passes() {
        assertThatCode(() -> Validate.Numbers.isBetween(5, 1, 10, "In range"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNumbers_IsBetween_AtLowerBound_Passes() {
        assertThatCode(() -> Validate.Numbers.isBetween(1, 1, 10, "At lower bound"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNumbers_IsBetween_AtUpperBound_Passes() {
        assertThatCode(() -> Validate.Numbers.isBetween(10, 1, 10, "At upper bound"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testNumbers_IsBetween_OutOfRange_Fails() {
        assertThatThrownBy(() -> Validate.Numbers.isBetween(15, 1, 10, "Out of range"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Soft Assertion Mode Tests
    // ===========================================

    @Test
    public void testSoftAssertionMode_MultipleFailures_DoNotThrow() {
        Validate.setFailOnError(false);

        assertThatCode(() -> {
            Validate.Strings.areEqual("expected", "actual", "Soft 1");
            Validate.Objects.isNull("not null", "Soft 2");
            Validate.Booleans.isTrue(false, "Soft 3");
        }).doesNotThrowAnyException();
    }
}
