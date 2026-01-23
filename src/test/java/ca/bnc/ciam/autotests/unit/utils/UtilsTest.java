package ca.bnc.ciam.autotests.unit.utils;

import ca.bnc.ciam.autotests.utils.Utils;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Utils class.
 * Tests utility methods for string manipulation, date formatting, and object cloning.
 */
@Test(groups = "unit")
public class UtilsTest {

    // ===========================================
    // generateRunId Tests
    // ===========================================

    @Test
    public void testGenerateRunId_ReturnsNonNull() {
        String runId = Utils.generateRunId();
        assertThat(runId).isNotNull();
    }

    @Test
    public void testGenerateRunId_StartsWithRunPrefix() {
        String runId = Utils.generateRunId();
        assertThat(runId).startsWith("run-");
    }

    @Test
    public void testGenerateRunId_ContainsTimestamp() {
        String runId = Utils.generateRunId();
        // Format: run-yyyy-MM-dd_HH-mm-ss
        assertThat(runId).matches("run-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}");
    }

    // ===========================================
    // generateUUID Tests
    // ===========================================

    @Test
    public void testGenerateUUID_ReturnsNonNull() {
        String uuid = Utils.generateUUID();
        assertThat(uuid).isNotNull();
    }

    @Test
    public void testGenerateUUID_ReturnsValidUUIDFormat() {
        String uuid = Utils.generateUUID();
        assertThat(uuid).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
    }

    @Test
    public void testGenerateUUID_ReturnsUniqueValues() {
        String uuid1 = Utils.generateUUID();
        String uuid2 = Utils.generateUUID();
        assertThat(uuid1).isNotEqualTo(uuid2);
    }

    // ===========================================
    // getCurrentTimestamp Tests
    // ===========================================

    @Test
    public void testGetCurrentTimestamp_ReturnsNonNull() {
        String timestamp = Utils.getCurrentTimestamp();
        assertThat(timestamp).isNotNull();
    }

    @Test
    public void testGetCurrentTimestamp_MatchesExpectedFormat() {
        String timestamp = Utils.getCurrentTimestamp();
        assertThat(timestamp).matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}");
    }

    // ===========================================
    // getCurrentDateTime Tests
    // ===========================================

    @Test
    public void testGetCurrentDateTime_ReturnsNonNull() {
        String datetime = Utils.getCurrentDateTime();
        assertThat(datetime).isNotNull();
    }

    @Test
    public void testGetCurrentDateTime_MatchesExpectedFormat() {
        String datetime = Utils.getCurrentDateTime();
        assertThat(datetime).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    // ===========================================
    // getCurrentDateTimeISO Tests
    // ===========================================

    @Test
    public void testGetCurrentDateTimeISO_ReturnsNonNull() {
        String datetime = Utils.getCurrentDateTimeISO();
        assertThat(datetime).isNotNull();
    }

    @Test
    public void testGetCurrentDateTimeISO_ContainsTSeparator() {
        String datetime = Utils.getCurrentDateTimeISO();
        assertThat(datetime).contains("T");
    }

    // ===========================================
    // isEmpty Tests
    // ===========================================

    @Test
    public void testIsEmpty_NullString_ReturnsTrue() {
        assertThat(Utils.isEmpty(null)).isTrue();
    }

    @Test
    public void testIsEmpty_EmptyString_ReturnsTrue() {
        assertThat(Utils.isEmpty("")).isTrue();
    }

    @Test
    public void testIsEmpty_WhitespaceOnly_ReturnsTrue() {
        assertThat(Utils.isEmpty("   ")).isTrue();
    }

    @Test
    public void testIsEmpty_NonEmptyString_ReturnsFalse() {
        assertThat(Utils.isEmpty("text")).isFalse();
    }

    // ===========================================
    // isNotEmpty Tests
    // ===========================================

    @Test
    public void testIsNotEmpty_NullString_ReturnsFalse() {
        assertThat(Utils.isNotEmpty(null)).isFalse();
    }

    @Test
    public void testIsNotEmpty_EmptyString_ReturnsFalse() {
        assertThat(Utils.isNotEmpty("")).isFalse();
    }

    @Test
    public void testIsNotEmpty_NonEmptyString_ReturnsTrue() {
        assertThat(Utils.isNotEmpty("text")).isTrue();
    }

    // ===========================================
    // getOrDefault Tests
    // ===========================================

    @Test
    public void testGetOrDefault_NullValue_ReturnsDefault() {
        String result = Utils.getOrDefault(null, "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    public void testGetOrDefault_EmptyValue_ReturnsDefault() {
        String result = Utils.getOrDefault("", "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    public void testGetOrDefault_NonEmptyValue_ReturnsValue() {
        String result = Utils.getOrDefault("value", "default");
        assertThat(result).isEqualTo("value");
    }

    // ===========================================
    // sanitizeForFileName Tests
    // ===========================================

    @Test
    public void testSanitizeForFileName_NullInput_ReturnsUnknown() {
        String result = Utils.sanitizeForFileName(null);
        assertThat(result).isEqualTo("unknown");
    }

    @Test
    public void testSanitizeForFileName_ValidChars_ReturnsUnchanged() {
        String result = Utils.sanitizeForFileName("test-file_name");
        assertThat(result).isEqualTo("test-file_name");
    }

    @Test
    public void testSanitizeForFileName_SpecialChars_ReplacesWithUnderscore() {
        String result = Utils.sanitizeForFileName("test file!@#$%");
        assertThat(result).doesNotContain(" ", "!", "@", "#", "$", "%");
    }

    @Test
    public void testSanitizeForFileName_MultipleUnderscores_CollapsesToSingle() {
        String result = Utils.sanitizeForFileName("test___name");
        assertThat(result).doesNotContain("__");
    }

    // ===========================================
    // methodNameToDescription Tests
    // ===========================================

    @Test
    public void testMethodNameToDescription_NullInput_ReturnsEmpty() {
        String result = Utils.methodNameToDescription(null);
        assertThat(result).isEmpty();
    }

    @Test
    public void testMethodNameToDescription_WithPrefix_RemovesPrefix() {
        String result = Utils.methodNameToDescription("t001_Navigate_To_Page");
        assertThat(result).isEqualTo("Navigate To Page");
    }

    @Test
    public void testMethodNameToDescription_WithoutPrefix_ReplacesUnderscores() {
        String result = Utils.methodNameToDescription("Navigate_To_Page");
        assertThat(result).isEqualTo("Navigate To Page");
    }

    // ===========================================
    // getStepNumber Tests
    // ===========================================

    @Test
    public void testGetStepNumber_NullInput_ReturnsZero() {
        int result = Utils.getStepNumber(null);
        assertThat(result).isEqualTo(0);
    }

    @Test
    public void testGetStepNumber_WithPrefix_ReturnsNumber() {
        int result = Utils.getStepNumber("t001_Navigate_To_Page");
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testGetStepNumber_WithHigherNumber_ReturnsNumber() {
        int result = Utils.getStepNumber("t123_Some_Step");
        assertThat(result).isEqualTo(123);
    }

    @Test
    public void testGetStepNumber_WithoutPrefix_ReturnsZero() {
        int result = Utils.getStepNumber("Navigate_To_Page");
        assertThat(result).isEqualTo(0);
    }

    // ===========================================
    // formatDuration Tests
    // ===========================================

    @Test
    public void testFormatDuration_Milliseconds_ReturnsMs() {
        String result = Utils.formatDuration(500);
        assertThat(result).isEqualTo("500 ms");
    }

    @Test
    public void testFormatDuration_Seconds_ReturnsSec() {
        String result = Utils.formatDuration(5000);
        assertThat(result).contains("sec");
    }

    @Test
    public void testFormatDuration_Minutes_ReturnsMinSec() {
        String result = Utils.formatDuration(90000);
        assertThat(result).contains("min");
    }

    @Test
    public void testFormatDuration_Hours_ReturnsHrMin() {
        String result = Utils.formatDuration(3700000);
        assertThat(result).contains("hr");
    }

    // ===========================================
    // cloneObject Tests
    // ===========================================

    @Test
    public void testCloneObject_NullInput_ReturnsNull() {
        Object result = Utils.cloneObject(null);
        assertThat(result).isNull();
    }

    @Test
    public void testCloneObject_SimpleObject_ClonesFields() {
        TestObject original = new TestObject("value", 42);
        TestObject cloned = Utils.cloneObject(original);

        assertThat(cloned).isNotNull();
        assertThat(cloned).isNotSameAs(original);
        assertThat(cloned.name).isEqualTo(original.name);
        assertThat(cloned.value).isEqualTo(original.value);
    }

    @Test
    public void testCloneObject_ObjectWithoutDefaultConstructor_ThrowsException() {
        ObjectWithoutDefaultConstructor obj = new ObjectWithoutDefaultConstructor("test");
        assertThatThrownBy(() -> Utils.cloneObject(obj))
                .isInstanceOf(RuntimeException.class);
    }

    // ===========================================
    // getEnvOrProperty Tests
    // ===========================================

    @Test
    public void testGetEnvOrProperty_ExistingEnvVar_ReturnsValue() {
        // PATH is typically always set
        String result = Utils.getEnvOrProperty("PATH", "default");
        assertThat(result).isNotEqualTo("default");
    }

    @Test
    public void testGetEnvOrProperty_NonExistentKey_ReturnsDefault() {
        String result = Utils.getEnvOrProperty("NONEXISTENT_KEY_12345", "default");
        assertThat(result).isEqualTo("default");
    }

    @Test
    public void testGetEnvOrProperty_NonExistentKeyNoDefault_ReturnsNull() {
        String result = Utils.getEnvOrProperty("NONEXISTENT_KEY_12345");
        assertThat(result).isNull();
    }

    // ===========================================
    // Test Helper Classes
    // ===========================================

    public static class TestObject {
        public String name;
        public int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    public static class ObjectWithoutDefaultConstructor {
        public String name;

        public ObjectWithoutDefaultConstructor(String name) {
            this.name = name;
        }
    }
}
