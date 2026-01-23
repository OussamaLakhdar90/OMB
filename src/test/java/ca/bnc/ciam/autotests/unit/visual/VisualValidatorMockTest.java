package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.VisualValidator;
import ca.bnc.ciam.autotests.visual.VisualValidator.VisualConfig;
import ca.bnc.ciam.autotests.visual.VisualValidator.VisualValidationResult;
import ca.bnc.ciam.autotests.visual.model.MismatchBehavior;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for VisualValidator with WebDriver mocking.
 * Tests screenshot capture and validation workflows.
 */
@Test(groups = "unit")
public class VisualValidatorMockTest {

    private VisualValidator validator;
    private WebDriver mockDriver;
    private TakesScreenshot mockScreenshotter;
    private Path testBaselineDir;

    @BeforeMethod
    public void setUp() throws IOException {
        validator = new VisualValidator();

        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(TakesScreenshot.class));
        mockScreenshotter = (TakesScreenshot) mockDriver;

        testBaselineDir = Paths.get("src/test/resources/baselines");
        Files.createDirectories(testBaselineDir);
    }

    // ===========================================
    // VisualConfig Builder Tests
    // ===========================================

    @Test
    public void testVisualConfig_Builder_Default() {
        VisualConfig config = VisualConfig.builder().build();

        assertThat(config.getBaselinePath()).isEqualTo("src/test/resources/baselines");
        assertThat(config.getDefaultTolerance()).isEqualTo(0.01);
        assertThat(config.isAutoCreateBaseline()).isTrue();
    }

    @Test
    public void testVisualConfig_Builder_CustomTolerance() {
        VisualConfig config = VisualConfig.builder()
                .defaultTolerance(0.05)
                .build();

        assertThat(config.getDefaultTolerance()).isEqualTo(0.05);
    }

    @Test
    public void testVisualConfig_Builder_CustomBaselinePath() {
        VisualConfig config = VisualConfig.builder()
                .baselinePath("custom/path")
                .build();

        assertThat(config.getBaselinePath()).isEqualTo("custom/path");
    }

    @Test
    public void testVisualConfig_Builder_AutoCreateDisabled() {
        VisualConfig config = VisualConfig.builder()
                .autoCreateBaseline(false)
                .build();

        assertThat(config.isAutoCreateBaseline()).isFalse();
    }

    // ===========================================
    // ValidationResult Static Factory Tests
    // ===========================================

    @Test
    public void testValidationResult_Success_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.success("baseline", null);

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.SUCCESS);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_Failure_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.failure("baseline", "Error message", null);

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.FAILURE);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.shouldFail()).isTrue();
    }

    @Test
    public void testValidationResult_Warning_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.warning("baseline", "Warning message", null);

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.WARNING);
        assertThat(result.shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_BaselineCreated_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.baselineCreated("test/baseline.png");

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.BASELINE_CREATED);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_Skipped_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.skipped("Skip reason");

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.SKIPPED);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_Ignored_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.ignored("Ignore reason");

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.IGNORED);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_Error_HasCorrectStatus() {
        VisualValidationResult result = VisualValidationResult.error("Error occurred");

        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.ERROR);
        assertThat(result.isSuccess()).isFalse();
    }

    // ===========================================
    // Screenshot Type Tests
    // ===========================================

    @Test
    public void testScreenshotType_Viewport_Exists() {
        assertThat(ScreenshotType.VIEWPORT).isNotNull();
    }

    @Test
    public void testScreenshotType_FullPage_Exists() {
        assertThat(ScreenshotType.FULL_PAGE).isNotNull();
    }

    @Test
    public void testScreenshotType_Element_Exists() {
        assertThat(ScreenshotType.ELEMENT).isNotNull();
    }

    @Test
    public void testScreenshotType_Both_Exists() {
        assertThat(ScreenshotType.BOTH).isNotNull();
    }

    // ===========================================
    // MismatchBehavior Tests
    // ===========================================

    @Test
    public void testMismatchBehavior_Fail_Exists() {
        assertThat(MismatchBehavior.FAIL).isNotNull();
    }

    @Test
    public void testMismatchBehavior_Warn_Exists() {
        assertThat(MismatchBehavior.WARN).isNotNull();
    }

    @Test
    public void testMismatchBehavior_Ignore_Exists() {
        assertThat(MismatchBehavior.IGNORE).isNotNull();
    }

    @Test
    public void testMismatchBehavior_Default_Exists() {
        assertThat(MismatchBehavior.DEFAULT).isNotNull();
    }

    // ===========================================
    // Configuration Tests
    // ===========================================

    @Test
    public void testConstructor_Default_CreatesValidator() {
        VisualValidator defaultValidator = new VisualValidator();
        assertThat(defaultValidator).isNotNull();
    }

    @Test
    public void testConstructor_WithConfig_CreatesValidator() {
        VisualConfig config = VisualConfig.builder()
                .defaultTolerance(0.02)
                .defaultScreenshotType(ScreenshotType.VIEWPORT)
                .build();

        VisualValidator customValidator = new VisualValidator(config);
        assertThat(customValidator).isNotNull();
    }

    @Test
    public void testConstructor_WithAllCustomConfig_CreatesValidator() {
        VisualConfig config = VisualConfig.builder()
                .baselinePath("custom/baselines")
                .defaultTolerance(0.03)
                .defaultScreenshotType(ScreenshotType.ELEMENT)
                .defaultMismatchBehavior(MismatchBehavior.WARN)
                .autoCreateBaseline(false)
                .build();

        VisualValidator customValidator = new VisualValidator(config);
        assertThat(customValidator).isNotNull();
    }

    // ===========================================
    // ValidationResult isSuccess Tests
    // ===========================================

    @Test
    public void testValidationResult_IsSuccess_TrueForSuccessStatus() {
        assertThat(VisualValidationResult.success("baseline", null).isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_IsSuccess_TrueForBaselineCreated() {
        assertThat(VisualValidationResult.baselineCreated("/path").isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_IsSuccess_TrueForSkipped() {
        assertThat(VisualValidationResult.skipped("reason").isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_IsSuccess_TrueForIgnored() {
        assertThat(VisualValidationResult.ignored("reason").isSuccess()).isTrue();
    }

    @Test
    public void testValidationResult_IsSuccess_FalseForFailure() {
        assertThat(VisualValidationResult.failure("baseline", "Error", null).isSuccess()).isFalse();
    }

    @Test
    public void testValidationResult_IsSuccess_FalseForWarning() {
        assertThat(VisualValidationResult.warning("baseline", "Warning", null).isSuccess()).isFalse();
    }

    @Test
    public void testValidationResult_IsSuccess_FalseForError() {
        assertThat(VisualValidationResult.error("Error").isSuccess()).isFalse();
    }

    // ===========================================
    // ValidationResult shouldFail Tests
    // ===========================================

    @Test
    public void testValidationResult_ShouldFail_TrueForFailure() {
        assertThat(VisualValidationResult.failure("baseline", "Error", null).shouldFail()).isTrue();
    }

    @Test
    public void testValidationResult_ShouldFail_FalseForSuccess() {
        assertThat(VisualValidationResult.success("baseline", null).shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_ShouldFail_FalseForWarning() {
        assertThat(VisualValidationResult.warning("baseline", "Warning", null).shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_ShouldFail_FalseForSkipped() {
        assertThat(VisualValidationResult.skipped("reason").shouldFail()).isFalse();
    }

    @Test
    public void testValidationResult_ShouldFail_FalseForBaselineCreated() {
        assertThat(VisualValidationResult.baselineCreated("/path").shouldFail()).isFalse();
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    private BufferedImage createTestImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private byte[] bufferedImageToBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
