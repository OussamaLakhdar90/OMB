package ca.bnc.ciam.autotests.unit.utils;

import ca.bnc.ciam.autotests.annotation.HasToBeIgnoredForAssertion;
import ca.bnc.ciam.autotests.utils.AssertionHelper;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for AssertionHelper.
 * Tests recursive field-by-field comparison.
 */
@Test(groups = "unit")
public class AssertionHelperTest {

    // ===========================================
    // Test Classes
    // ===========================================

    public static class PersonResponse {
        private String name;
        private int age;
        private AddressResponse address;

        public PersonResponse() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public AddressResponse getAddress() {
            return address;
        }

        public void setAddress(AddressResponse address) {
            this.address = address;
        }
    }

    public static class AddressResponse {
        private String street;
        private String city;

        public AddressResponse() {
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    public static class ResponseWithIgnoredField {
        private String value;
        @HasToBeIgnoredForAssertion
        private String timestamp;

        public ResponseWithIgnoredField() {
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class NumericResponse {
        private double score;
        private float rating;

        public NumericResponse() {
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public float getRating() {
            return rating;
        }

        public void setRating(float rating) {
            this.rating = rating;
        }
    }

    // ===========================================
    // compareFieldByFieldRecursively Tests
    // ===========================================

    @Test
    public void testCompareFieldByFieldRecursively_EqualSimpleObjects_Passes() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAge(30);

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAge(30);

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Test context"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_EqualNestedObjects_Passes() {
        AddressResponse address1 = new AddressResponse();
        address1.setStreet("123 Main St");
        address1.setCity("Boston");

        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAge(30);
        actual.setAddress(address1);

        AddressResponse address2 = new AddressResponse();
        address2.setStreet("123 Main St");
        address2.setCity("Boston");

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAge(30);
        expected.setAddress(address2);

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Nested test"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_DifferentSimpleField_Fails() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAge(30);

        PersonResponse expected = new PersonResponse();
        expected.setName("Jane");
        expected.setAge(30);

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Mismatch test"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testCompareFieldByFieldRecursively_DifferentNestedField_Fails() {
        AddressResponse address1 = new AddressResponse();
        address1.setStreet("123 Main St");
        address1.setCity("Boston");

        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAddress(address1);

        AddressResponse address2 = new AddressResponse();
        address2.setStreet("456 Oak Ave");
        address2.setCity("Boston");

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAddress(address2);

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Nested mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // Ignored Fields Tests
    // ===========================================

    @Test
    public void testCompareFieldByFieldRecursively_IgnoredFieldDifferent_Passes() {
        ResponseWithIgnoredField actual = new ResponseWithIgnoredField();
        actual.setValue("test");
        actual.setTimestamp("2024-01-01T10:00:00");

        ResponseWithIgnoredField expected = new ResponseWithIgnoredField();
        expected.setValue("test");
        expected.setTimestamp("2024-12-31T23:59:59");

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Ignored field test"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_NonIgnoredFieldDifferent_Fails() {
        ResponseWithIgnoredField actual = new ResponseWithIgnoredField();
        actual.setValue("actual");
        actual.setTimestamp("2024-01-01T10:00:00");

        ResponseWithIgnoredField expected = new ResponseWithIgnoredField();
        expected.setValue("expected");
        expected.setTimestamp("2024-01-01T10:00:00");

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Value mismatch"))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // compareFieldByFieldRecursively with extra ignored fields
    // ===========================================

    @Test
    public void testCompareFieldByFieldRecursively_WithExtraIgnoredFields_Passes() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAge(30);

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAge(25); // Different but ignored

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Extra ignored", "age"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_WithMultipleExtraIgnoredFields_Passes() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAge(30);

        PersonResponse expected = new PersonResponse();
        expected.setName("Jane"); // Different but ignored
        expected.setAge(25);      // Different but ignored

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Multiple ignored", "name", "age"))
                .doesNotThrowAnyException();
    }

    // ===========================================
    // compareWithTolerance Tests
    // ===========================================

    @Test
    public void testCompareWithTolerance_WithinTolerance_Passes() {
        NumericResponse actual = new NumericResponse();
        actual.setScore(99.99);
        actual.setRating(4.5f);

        NumericResponse expected = new NumericResponse();
        expected.setScore(100.0);
        expected.setRating(4.5f);

        assertThatCode(() -> AssertionHelper.compareWithTolerance(expected, actual, "Tolerance test", 0.1))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareWithTolerance_OutsideTolerance_Fails() {
        NumericResponse actual = new NumericResponse();
        actual.setScore(95.0);
        actual.setRating(4.5f);

        NumericResponse expected = new NumericResponse();
        expected.setScore(100.0);
        expected.setRating(4.5f);

        assertThatThrownBy(() -> AssertionHelper.compareWithTolerance(expected, actual, "Tolerance fail", 0.1))
                .isInstanceOf(AssertionError.class);
    }

    // ===========================================
    // cloneForAssertion Tests
    // ===========================================

    @Test
    public void testCloneForAssertion_NullInput_ReturnsNull() {
        Object result = AssertionHelper.cloneForAssertion(null);
        assertThat(result).isNull();
    }

    @Test
    public void testCloneForAssertion_SimpleObject_ClonesFields() {
        PersonResponse original = new PersonResponse();
        original.setName("John");
        original.setAge(30);

        PersonResponse cloned = AssertionHelper.cloneForAssertion(original);

        assertThat(cloned).isNotNull();
        assertThat(cloned).isNotSameAs(original);
        assertThat(cloned.getName()).isEqualTo(original.getName());
        assertThat(cloned.getAge()).isEqualTo(original.getAge());
    }

    @Test
    public void testCloneForAssertion_ObjectWithNestedObject_ClonesShallow() {
        AddressResponse address = new AddressResponse();
        address.setStreet("123 Main St");

        PersonResponse original = new PersonResponse();
        original.setName("John");
        original.setAddress(address);

        PersonResponse cloned = AssertionHelper.cloneForAssertion(original);

        assertThat(cloned).isNotNull();
        assertThat(cloned.getName()).isEqualTo(original.getName());
        // Shallow clone - same reference
        assertThat(cloned.getAddress()).isSameAs(original.getAddress());
    }

    // ===========================================
    // Null Values Tests
    // ===========================================

    @Test
    public void testCompareFieldByFieldRecursively_BothNull_Passes() {
        PersonResponse actual = new PersonResponse();
        actual.setName(null);

        PersonResponse expected = new PersonResponse();
        expected.setName(null);

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Both null"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_ActualNull_Fails() {
        PersonResponse actual = new PersonResponse();
        actual.setName(null);

        PersonResponse expected = new PersonResponse();
        expected.setName("John");

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Actual null"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testCompareFieldByFieldRecursively_ExpectedNull_Fails() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");

        PersonResponse expected = new PersonResponse();
        expected.setName(null);

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Expected null"))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    public void testCompareFieldByFieldRecursively_NullNestedObject_BothNull_Passes() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAddress(null);

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAddress(null);

        assertThatCode(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Nested null"))
                .doesNotThrowAnyException();
    }

    @Test
    public void testCompareFieldByFieldRecursively_NullNestedObject_OnlyActualNull_Fails() {
        PersonResponse actual = new PersonResponse();
        actual.setName("John");
        actual.setAddress(null);

        AddressResponse address = new AddressResponse();
        address.setStreet("123 Main St");

        PersonResponse expected = new PersonResponse();
        expected.setName("John");
        expected.setAddress(address);

        assertThatThrownBy(() -> AssertionHelper.compareFieldByFieldRecursively(expected, actual, "Nested null mismatch"))
                .isInstanceOf(AssertionError.class);
    }
}
