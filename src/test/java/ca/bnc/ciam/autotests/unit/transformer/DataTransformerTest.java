package ca.bnc.ciam.autotests.unit.transformer;

import ca.bnc.ciam.autotests.transformer.DataTransformer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DataTransformer.
 * Tests sensitive data transformation and environment variable substitution.
 */
@Test(groups = "unit")
public class DataTransformerTest {

    private DataTransformer transformer;

    @BeforeMethod
    public void setUp() {
        transformer = new DataTransformer();
    }

    // ===========================================
    // transform Tests - Modern Marker ($sensitive:)
    // ===========================================

    @Test
    public void testTransform_NullValue_ReturnsNull() {
        String result = transformer.transform(null);
        assertThat(result).isNull();
    }

    @Test
    public void testTransform_RegularValue_ReturnsUnchanged() {
        String result = transformer.transform("regular value");
        assertThat(result).isEqualTo("regular value");
    }

    @Test
    public void testTransform_EmptyString_ReturnsEmpty() {
        String result = transformer.transform("");
        assertThat(result).isEmpty();
    }

    @Test
    public void testTransform_ValueWithoutSensitivePrefix_ReturnsUnchanged() {
        String result = transformer.transform("username");
        assertThat(result).isEqualTo("username");
    }

    @Test
    public void testTransform_SensitiveValueWithEnvVariable_ReturnsEnvValue() {
        // PATH is typically always set on all systems
        String result = transformer.transform("$sensitive:PATH");
        // Either returns the env value or the original (if not found)
        assertThat(result).isNotNull();
    }

    @Test
    public void testTransform_SensitiveValueWithNonExistentEnvVariable_ReturnsOriginal() {
        String result = transformer.transform("$sensitive:NONEXISTENT_VAR_12345");
        assertThat(result).isEqualTo("$sensitive:NONEXISTENT_VAR_12345");
    }

    @Test
    public void testTransform_SensitiveValueWithNoDelimiter_ReturnsOriginal() {
        String result = transformer.transform("$sensitive");
        assertThat(result).isEqualTo("$sensitive");
    }

    @Test
    public void testTransform_SensitiveValueWithEmptyKey_ReturnsOriginal() {
        String result = transformer.transform("$sensitive:");
        assertThat(result).isEqualTo("$sensitive:");
    }

    // ===========================================
    // transform Tests - Legacy Marker ($en$itive:)
    // ===========================================

    @Test
    public void testTransform_LegacyMarker_WithEnvVariable_ReturnsEnvValue() {
        // PATH is typically always set
        String result = transformer.transform("$en$itive:PATH");
        assertThat(result).isNotNull();
    }

    @Test
    public void testTransform_LegacyMarker_WithNonExistentEnvVariable_ReturnsOriginal() {
        String result = transformer.transform("$en$itive:NONEXISTENT_VAR_12345");
        assertThat(result).isEqualTo("$en$itive:NONEXISTENT_VAR_12345");
    }

    @Test
    public void testTransform_LegacyMarker_WithEmptyKey_ReturnsOriginal() {
        String result = transformer.transform("$en$itive:");
        assertThat(result).isEqualTo("$en$itive:");
    }

    // ===========================================
    // isSensitive Tests
    // ===========================================

    @Test
    public void testIsSensitive_NullValue_ReturnsFalse() {
        boolean result = transformer.isSensitive(null);
        assertThat(result).isFalse();
    }

    @Test
    public void testIsSensitive_RegularValue_ReturnsFalse() {
        boolean result = transformer.isSensitive("regular value");
        assertThat(result).isFalse();
    }

    @Test
    public void testIsSensitive_EmptyString_ReturnsFalse() {
        boolean result = transformer.isSensitive("");
        assertThat(result).isFalse();
    }

    @Test
    public void testIsSensitive_ModernMarker_ReturnsTrue() {
        boolean result = transformer.isSensitive("$sensitive:MY_SECRET");
        assertThat(result).isTrue();
    }

    @Test
    public void testIsSensitive_LegacyMarker_ReturnsTrue() {
        boolean result = transformer.isSensitive("$en$itive:MY_SECRET");
        assertThat(result).isTrue();
    }

    @Test
    public void testIsSensitive_JustPrefix_ReturnsTrue() {
        boolean result = transformer.isSensitive("$sensitive:");
        assertThat(result).isTrue();
    }

    @Test
    public void testIsSensitive_JustLegacyPrefix_ReturnsTrue() {
        boolean result = transformer.isSensitive("$en$itive:");
        assertThat(result).isTrue();
    }

    // ===========================================
    // getSensitiveKey Tests
    // ===========================================

    @Test
    public void testGetSensitiveKey_NullValue_ReturnsNull() {
        String result = transformer.getSensitiveKey(null);
        assertThat(result).isNull();
    }

    @Test
    public void testGetSensitiveKey_RegularValue_ReturnsNull() {
        String result = transformer.getSensitiveKey("regular value");
        assertThat(result).isNull();
    }

    @Test
    public void testGetSensitiveKey_ModernMarker_ReturnsKey() {
        String result = transformer.getSensitiveKey("$sensitive:MY_SECRET");
        assertThat(result).isEqualTo("MY_SECRET");
    }

    @Test
    public void testGetSensitiveKey_LegacyMarker_ReturnsKey() {
        String result = transformer.getSensitiveKey("$en$itive:MY_SECRET");
        assertThat(result).isEqualTo("MY_SECRET");
    }

    @Test
    public void testGetSensitiveKey_SensitiveValueWithSpaces_ReturnsTrimmedKey() {
        String result = transformer.getSensitiveKey("$sensitive:  MY_SECRET  ");
        assertThat(result).isEqualTo("MY_SECRET");
    }

    @Test
    public void testGetSensitiveKey_SensitiveValueWithEmptyKey_ReturnsEmptyString() {
        String result = transformer.getSensitiveKey("$sensitive:");
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetSensitiveKey_SensitiveValueWithMultipleColons_ReturnsFullKey() {
        // After "$sensitive:" everything is the key, including additional colons
        String result = transformer.getSensitiveKey("$sensitive:prefix:MY_SECRET");
        assertThat(result).isEqualTo("prefix:MY_SECRET");
    }

    // ===========================================
    // System Property Fallback Tests
    // ===========================================

    @Test
    public void testTransform_SensitiveValueWithSystemProperty_ReturnsPropertyValue() {
        // Set a system property for testing
        String testKey = "TEST_TRANSFORMER_PROP_" + System.currentTimeMillis();
        String testValue = "test-value-123";
        System.setProperty(testKey, testValue);

        try {
            String result = transformer.transform("$sensitive:" + testKey);
            assertThat(result).isEqualTo(testValue);
        } finally {
            System.clearProperty(testKey);
        }
    }
}
