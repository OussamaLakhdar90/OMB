package ca.bnc.ciam.autotests.unit.utils;

import ca.bnc.ciam.autotests.utils.VisualCapture;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VisualCapture utility class.
 * Tests the record mode detection and configuration.
 */
@Test(groups = "unit")
public class VisualCaptureTest {

    private String originalRecordMode;

    @BeforeMethod
    public void setUp() {
        // Save original value
        originalRecordMode = System.getProperty("bnc.record.mode");
    }

    @AfterMethod
    public void tearDown() {
        // Restore original value
        if (originalRecordMode != null) {
            System.setProperty("bnc.record.mode", originalRecordMode);
        } else {
            System.clearProperty("bnc.record.mode");
        }
    }

    // ===========================================
    // Record Mode Detection Tests
    // ===========================================

    @Test
    public void testIsRecordMode_WhenTrue_ReturnsTrue() {
        System.setProperty("bnc.record.mode", "true");
        assertThat(VisualCapture.isRecordMode()).isTrue();
    }

    @Test
    public void testIsRecordMode_WhenTrueUpperCase_ReturnsTrue() {
        System.setProperty("bnc.record.mode", "TRUE");
        assertThat(VisualCapture.isRecordMode()).isTrue();
    }

    @Test
    public void testIsRecordMode_WhenTrueMixedCase_ReturnsTrue() {
        System.setProperty("bnc.record.mode", "True");
        assertThat(VisualCapture.isRecordMode()).isTrue();
    }

    @Test
    public void testIsRecordMode_WhenFalse_ReturnsFalse() {
        System.setProperty("bnc.record.mode", "false");
        assertThat(VisualCapture.isRecordMode()).isFalse();
    }

    @Test
    public void testIsRecordMode_WhenNotSet_ReturnsFalse() {
        System.clearProperty("bnc.record.mode");
        assertThat(VisualCapture.isRecordMode()).isFalse();
    }

    @Test
    public void testIsRecordMode_WhenEmptyString_ReturnsFalse() {
        System.setProperty("bnc.record.mode", "");
        assertThat(VisualCapture.isRecordMode()).isFalse();
    }

    @Test
    public void testIsRecordMode_WhenInvalidValue_ReturnsFalse() {
        System.setProperty("bnc.record.mode", "invalid");
        assertThat(VisualCapture.isRecordMode()).isFalse();
    }

    // ===========================================
    // Last Diff Base64 Tests
    // ===========================================

    @Test
    public void testGetLastDiffBase64_WhenNoDiff_ReturnsNull() {
        // Without running a capture, the last diff should be null
        assertThat(VisualCapture.getLastDiffBase64()).isNull();
    }

    // ===========================================
    // Last Error Message Tests
    // ===========================================

    @Test
    public void testGetLastErrorMessage_WhenNoError_ReturnsNull() {
        // Without running a capture, the last error should be null
        assertThat(VisualCapture.getLastErrorMessage()).isNull();
    }

    // ===========================================
    // CaptureStep without WebDriver Tests
    // ===========================================

    @Test
    public void testCaptureStep_WithNullDriver_ReturnsFalse() {
        System.setProperty("bnc.record.mode", "false");

        // Should handle null driver gracefully and return false
        boolean result = VisualCapture.captureStep(null, "TestClass", "testStep");
        assertThat(result).isFalse();
    }

    @Test
    public void testCaptureStep_RecordModeWithNullDriver_ReturnsFalse() {
        System.setProperty("bnc.record.mode", "true");

        // Even in record mode, null driver should return false
        boolean result = VisualCapture.captureStep(null, "TestClass", "testStep");
        assertThat(result).isFalse();
    }
}
