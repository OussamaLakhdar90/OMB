package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.VisualValidator;
import ca.bnc.ciam.autotests.visual.VisualValidator.VisualConfig;
import ca.bnc.ciam.autotests.visual.VisualValidator.VisualValidationResult;
import ca.bnc.ciam.autotests.visual.model.MismatchBehavior;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VisualValidator.
 * Tests the visual validation result handling and configuration.
 */
@Test(groups = "unit")
public class VisualValidatorTest {

    private VisualValidator validator;
    private Path testBaselineDir;
    private Path testActualDir;

    @BeforeMethod
    public void setUp() throws IOException {
        validator = new VisualValidator();
        testBaselineDir = Paths.get("target/test-visual/baselines");
        testActualDir = Paths.get("target/test-visual/actual");
        Files.createDirectories(testBaselineDir);
        Files.createDirectories(testActualDir);
    }

    // ===========================================
    // VisualValidationResult Tests
    // ===========================================

    @Test
    public void testValidationResult_Success_IsSuccess() {
        VisualValidationResult result = VisualValidationResult.success("baseline", null);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.SUCCESS);
    }

    @Test
    public void testValidationResult_Failure_ShouldFail() {
        VisualValidationResult result = VisualValidationResult.failure("baseline", "Mismatch detected", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.shouldFail()).isTrue();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.FAILURE);
        assertThat(result.getMessage()).isEqualTo("Mismatch detected");
    }

    @Test
    public void testValidationResult_Warning_IsSuccess() {
        VisualValidationResult result = VisualValidationResult.warning("baseline", "Minor diff", null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.shouldFail()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.WARNING);
    }

    @Test
    public void testValidationResult_Skipped_IsSuccess() {
        VisualValidationResult result = VisualValidationResult.skipped("Test skipped");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.SKIPPED);
    }

    @Test
    public void testValidationResult_Ignored_IsSuccess() {
        VisualValidationResult result = VisualValidationResult.ignored("Ignored mismatch");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.IGNORED);
    }

    @Test
    public void testValidationResult_BaselineCreated_IsSuccess() {
        VisualValidationResult result = VisualValidationResult.baselineCreated("/path/to/baseline.png");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.shouldFail()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.BASELINE_CREATED);
        assertThat(result.getMessage()).contains("Baseline created");
    }

    @Test
    public void testValidationResult_Error_ShouldNotBeSuccess() {
        VisualValidationResult result = VisualValidationResult.error("IOException occurred");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getStatus()).isEqualTo(VisualValidationResult.Status.ERROR);
    }

    // ===========================================
    // VisualConfig Tests
    // ===========================================

    @Test
    public void testVisualConfig_DefaultValues() {
        VisualConfig config = VisualConfig.builder().build();

        assertThat(config.getBaselinePath()).isEqualTo("src/test/resources/baselines");
        assertThat(config.getDefaultTolerance()).isEqualTo(0.01);
        assertThat(config.getDefaultScreenshotType()).isEqualTo(ScreenshotType.FULL_PAGE);
        assertThat(config.getDefaultMismatchBehavior()).isEqualTo(MismatchBehavior.FAIL);
        assertThat(config.isAutoCreateBaseline()).isTrue();
    }

    @Test
    public void testVisualConfig_CustomValues() {
        VisualConfig config = VisualConfig.builder()
                .baselinePath("custom/baselines")
                .defaultTolerance(0.05)
                .defaultScreenshotType(ScreenshotType.VIEWPORT)
                .defaultMismatchBehavior(MismatchBehavior.WARN)
                .autoCreateBaseline(false)
                .build();

        assertThat(config.getBaselinePath()).isEqualTo("custom/baselines");
        assertThat(config.getDefaultTolerance()).isEqualTo(0.05);
        assertThat(config.getDefaultScreenshotType()).isEqualTo(ScreenshotType.VIEWPORT);
        assertThat(config.getDefaultMismatchBehavior()).isEqualTo(MismatchBehavior.WARN);
        assertThat(config.isAutoCreateBaseline()).isFalse();
    }

    // ===========================================
    // ScreenshotType Tests
    // ===========================================

    @Test
    public void testScreenshotType_AllValuesExist() {
        assertThat(ScreenshotType.FULL_PAGE).isNotNull();
        assertThat(ScreenshotType.VIEWPORT).isNotNull();
        assertThat(ScreenshotType.ELEMENT).isNotNull();
        assertThat(ScreenshotType.BOTH).isNotNull();
    }

    // ===========================================
    // MismatchBehavior Tests
    // ===========================================

    @Test
    public void testMismatchBehavior_AllValuesExist() {
        assertThat(MismatchBehavior.FAIL).isNotNull();
        assertThat(MismatchBehavior.WARN).isNotNull();
        assertThat(MismatchBehavior.IGNORE).isNotNull();
        assertThat(MismatchBehavior.DEFAULT).isNotNull();
    }

    // ===========================================
    // VisualValidator Constructor Tests
    // ===========================================

    @Test
    public void testDefaultConstructor_CreatesValidInstance() {
        VisualValidator defaultValidator = new VisualValidator();
        assertThat(defaultValidator).isNotNull();
    }

    @Test
    public void testConstructor_WithConfig_CreatesValidInstance() {
        VisualConfig config = VisualConfig.builder()
                .defaultTolerance(0.02)
                .build();
        VisualValidator customValidator = new VisualValidator(config);
        assertThat(customValidator).isNotNull();
    }

    // ===========================================
    // Image Comparison Helper Tests
    // ===========================================

    @Test
    public void testIdenticalImages_ShouldMatch() throws IOException {
        BufferedImage image1 = createTestImage(100, 100, Color.BLUE);
        BufferedImage image2 = createTestImage(100, 100, Color.BLUE);

        Path baselinePath = testBaselineDir.resolve("TestClass/identical_test.png");
        Path actualPath = testActualDir.resolve("TestClass/identical_test.png");
        Files.createDirectories(baselinePath.getParent());
        Files.createDirectories(actualPath.getParent());

        ImageIO.write(image1, "png", baselinePath.toFile());
        ImageIO.write(image2, "png", actualPath.toFile());

        assertThat(imagesAreIdentical(image1, image2)).isTrue();
    }

    @Test
    public void testDifferentColorImages_ShouldNotMatch() {
        BufferedImage image1 = createTestImage(100, 100, Color.BLUE);
        BufferedImage image2 = createTestImage(100, 100, Color.RED);

        assertThat(imagesAreIdentical(image1, image2)).isFalse();
    }

    @Test
    public void testSlightlyDifferentImages_WithinTolerance() {
        assertThat(colorsAreWithinThreshold(Color.BLUE.getRGB(), new Color(0, 0, 252).getRGB(), 5)).isTrue();
    }

    @Test
    public void testDifferentImages_OutsideTolerance() {
        assertThat(colorsAreWithinThreshold(Color.BLUE.getRGB(), new Color(0, 0, 200).getRGB(), 5)).isFalse();
    }

    // ===========================================
    // Edge Cases
    // ===========================================

    @Test
    public void testGetSimpleClassName_WithPackage() {
        String fullName = "ca.bnc.ciam.autotests.web.test.login.LoginTest1";
        String simpleName = getSimpleClassName(fullName);
        assertThat(simpleName).isEqualTo("LoginTest1");
    }

    @Test
    public void testGetSimpleClassName_WithoutPackage() {
        String simpleName = "LoginTest1";
        String result = getSimpleClassName(simpleName);
        assertThat(result).isEqualTo("LoginTest1");
    }

    @Test
    public void testGetSimpleClassName_Null() {
        String result = getSimpleClassName(null);
        assertThat(result).isEqualTo("Unknown");
    }

    // ===========================================
    // Status Enum Tests
    // ===========================================

    @Test
    public void testStatus_AllValuesExist() {
        assertThat(VisualValidationResult.Status.SUCCESS).isNotNull();
        assertThat(VisualValidationResult.Status.FAILURE).isNotNull();
        assertThat(VisualValidationResult.Status.WARNING).isNotNull();
        assertThat(VisualValidationResult.Status.SKIPPED).isNotNull();
        assertThat(VisualValidationResult.Status.IGNORED).isNotNull();
        assertThat(VisualValidationResult.Status.BASELINE_CREATED).isNotNull();
        assertThat(VisualValidationResult.Status.ERROR).isNotNull();
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

    private boolean imagesAreIdentical(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean colorsAreWithinThreshold(int rgb1, int rgb2, int threshold) {
        int r1 = (rgb1 >> 16) & 0xff;
        int g1 = (rgb1 >> 8) & 0xff;
        int b1 = rgb1 & 0xff;

        int r2 = (rgb2 >> 16) & 0xff;
        int g2 = (rgb2 >> 8) & 0xff;
        int b2 = rgb2 & 0xff;

        return Math.abs(r1 - r2) <= threshold &&
                Math.abs(g1 - g2) <= threshold &&
                Math.abs(b1 - b2) <= threshold;
    }

    private String getSimpleClassName(String testClass) {
        if (testClass == null) return "Unknown";
        return testClass.contains(".") ?
                testClass.substring(testClass.lastIndexOf('.') + 1) : testClass;
    }
}
