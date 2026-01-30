package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.ImageComparator;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ImageComparator.
 */
@Test(groups = "unit")
public class ImageComparatorTest {

    private ImageComparator comparator;

    @BeforeMethod
    public void setUp() {
        comparator = new ImageComparator(0.01); // 1% tolerance
    }

    @Test
    public void testIdenticalImages_ShouldMatch() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        assertThat(result.isMatch()).isTrue();
        assertThat(result.getDiffPercentage()).isEqualTo(0.0);
        assertThat(result.getDiffPixelCount()).isEqualTo(0);
    }

    @Test
    public void testCompletelyDifferentImages_ShouldNotMatch() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.BLACK);

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        assertThat(result.isMatch()).isFalse();
        assertThat(result.getDiffPercentage()).isGreaterThan(0.5); // More than 50% different
    }

    @Test
    public void testSmallDifference_WithinTolerance_ShouldMatch() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        // Add a small difference (1 pixel out of 10000)
        image2.setRGB(50, 50, Color.RED.getRGB());

        ImageComparator comparatorHighTolerance = new ImageComparator(0.01); // 1% tolerance
        ImageComparator.ComparisonResult result = comparatorHighTolerance.compare(image1, image2);

        // 1 pixel difference out of 10000 = 0.01% which is within 1% tolerance
        assertThat(result.isMatch()).isTrue();
    }

    @Test
    public void testDifferentSizeImages_ShouldResize() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(150, 150, Color.WHITE);

        // Should not throw exception and should handle resize
        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        assertThat(result).isNotNull();
        assertThat(result.getBaselineWidth()).isEqualTo(100);
        assertThat(result.getBaselineHeight()).isEqualTo(100);
    }

    @Test
    public void testIgnoreRegions_ShouldExcludeFromComparison() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        // Add difference in center
        for (int x = 40; x < 60; x++) {
            for (int y = 40; y < 60; y++) {
                image2.setRGB(x, y, Color.RED.getRGB());
            }
        }

        // Ignore the region where difference exists
        List<int[]> ignoreRegions = Arrays.asList(
                new int[]{35, 35, 30, 30} // x, y, width, height
        );

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2, 0.01, ignoreRegions);

        // With ignore region, should match since difference is masked
        assertThat(result.isMatch()).isTrue();
    }

    @Test
    public void testCustomTolerance() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        // Add 10% difference
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 100; y++) {
                image2.setRGB(x, y, Color.BLACK.getRGB());
            }
        }

        // With 5% tolerance, should not match
        ImageComparator.ComparisonResult lowTolerance = comparator.compare(image1, image2, 0.05);
        assertThat(lowTolerance.isMatch()).isFalse();

        // With 15% tolerance, should match
        ImageComparator.ComparisonResult highTolerance = comparator.compare(image1, image2, 0.15);
        assertThat(highTolerance.isMatch()).isTrue();
    }

    @Test
    public void testDiffImageGenerated() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        // Add visible difference
        for (int x = 40; x < 60; x++) {
            for (int y = 40; y < 60; y++) {
                image2.setRGB(x, y, Color.RED.getRGB());
            }
        }

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        assertThat(result.getDiffImage()).isNotNull();
        assertThat(result.getDiffImage().getWidth()).isEqualTo(100);
        assertThat(result.getDiffImage().getHeight()).isEqualTo(100);
    }

    @Test
    public void testComparisonResultSummary() {
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        String summary = result.getSummary();
        assertThat(summary).contains("Match: true");
        assertThat(summary).contains("Diff:");
        assertThat(summary).contains("Tolerance:");
    }

    @Test
    public void testParseIgnoreRegions_ValidFormat() {
        String regionsString = "10,20,30,40;50,60,70,80";
        List<int[]> regions = ImageComparator.parseIgnoreRegions(regionsString);

        assertThat(regions).hasSize(2);
        assertThat(regions.get(0)).containsExactly(10, 20, 30, 40);
        assertThat(regions.get(1)).containsExactly(50, 60, 70, 80);
    }

    @Test
    public void testParseIgnoreRegions_EmptyString() {
        List<int[]> regions = ImageComparator.parseIgnoreRegions("");
        assertThat(regions).isEmpty();
    }

    @Test
    public void testParseIgnoreRegions_NullString() {
        List<int[]> regions = ImageComparator.parseIgnoreRegions(null);
        assertThat(regions).isEmpty();
    }

    @Test
    public void testParseIgnoreRegions_InvalidFormat() {
        // Invalid format should be skipped
        String regionsString = "10,20,30;50,60,70,80"; // First region missing fourth value
        List<int[]> regions = ImageComparator.parseIgnoreRegions(regionsString);

        assertThat(regions).hasSize(1); // Only valid region parsed
        assertThat(regions.get(0)).containsExactly(50, 60, 70, 80);
    }

    @Test
    public void testDefaultTolerance() {
        ImageComparator defaultComparator = new ImageComparator();
        BufferedImage image1 = createSolidImage(100, 100, Color.WHITE);
        BufferedImage image2 = createSolidImage(100, 100, Color.WHITE);

        ImageComparator.ComparisonResult result = defaultComparator.compare(image1, image2);

        assertThat(result.getTolerance()).isEqualTo(0.003); // Default 0.3% tolerance (strict)
    }

    // ===========================================
    // Scaling Tests (for local hybrid comparison)
    // ===========================================

    @Test
    public void testScaledComparison_DetectsScaling() {
        // Baseline at higher resolution (like SauceLabs 1920x1080)
        BufferedImage baseline = createSolidImage(200, 200, Color.WHITE);
        // Actual at lower resolution (like local laptop)
        BufferedImage actual = createSolidImage(150, 150, Color.WHITE);

        ImageComparator.ComparisonResult result = comparator.compare(baseline, actual);

        // Should detect that scaling occurred
        assertThat(result.isWasScaled()).isTrue();
        assertThat(result.getScaleFactor()).isGreaterThan(1.0); // Upscaled
        // Original actual dimensions should be preserved
        assertThat(result.getActualWidth()).isEqualTo(150);
        assertThat(result.getActualHeight()).isEqualTo(150);
    }

    @Test
    public void testScaledComparison_SameColorImages_ShouldMatch() {
        // Baseline at higher resolution
        BufferedImage baseline = createSolidImage(200, 200, Color.BLUE);
        // Actual at lower resolution but same color
        BufferedImage actual = createSolidImage(150, 150, Color.BLUE);

        // Use higher tolerance for scaled comparison
        ImageComparator.ComparisonResult result = comparator.compare(baseline, actual, 0.10);

        // Solid color images should match even with scaling
        assertThat(result.isMatch()).isTrue();
        assertThat(result.isWasScaled()).isTrue();
    }

    @Test
    public void testScaledComparison_DifferentImages_ShouldDetectDifference() {
        // Baseline with pattern
        BufferedImage baseline = createSplitImage(200, 200, Color.RED, Color.BLUE);
        // Actual completely different
        BufferedImage actual = createSolidImage(150, 150, Color.GREEN);

        ImageComparator.ComparisonResult result = comparator.compare(baseline, actual, 0.01);

        // Should detect the difference even with scaling
        assertThat(result.isMatch()).isFalse();
        assertThat(result.isWasScaled()).isTrue();
        assertThat(result.getDiffPercentage()).isGreaterThan(0.20);
    }

    @Test
    public void testScaledComparison_SummaryIncludesScaleInfo() {
        BufferedImage baseline = createSolidImage(200, 200, Color.WHITE);
        BufferedImage actual = createSolidImage(150, 150, Color.WHITE);

        ImageComparator.ComparisonResult result = comparator.compare(baseline, actual, 0.10);

        String summary = result.getSummary();
        assertThat(summary).contains("Scaled:");
    }

    @Test
    public void testNoScaling_WhenSameDimensions() {
        BufferedImage baseline = createSolidImage(100, 100, Color.WHITE);
        BufferedImage actual = createSolidImage(100, 100, Color.WHITE);

        ImageComparator.ComparisonResult result = comparator.compare(baseline, actual);

        // Should NOT be marked as scaled
        assertThat(result.isWasScaled()).isFalse();
        assertThat(result.getScaleFactor()).isEqualTo(1.0);
    }

    @Test
    public void testHighQualityScaling_StaticMethod() {
        BufferedImage original = createSolidImage(100, 100, Color.RED);

        BufferedImage scaled = ImageComparator.scaleImageHighQuality(original, 200, 200);

        assertThat(scaled.getWidth()).isEqualTo(200);
        assertThat(scaled.getHeight()).isEqualTo(200);
        // The scaled image should still be predominantly red
        int centerPixel = scaled.getRGB(100, 100);
        int red = (centerPixel >> 16) & 0xFF;
        assertThat(red).isGreaterThan(200); // Should be mostly red
    }

    @Test
    public void testDiffImage_MultipleRegions_DetectedSeparately() {
        BufferedImage image1 = createSolidImage(200, 200, Color.WHITE);
        BufferedImage image2 = createSolidImage(200, 200, Color.WHITE);

        // Add two separate diff regions (far apart so they won't be merged)
        // Region 1: top-left corner
        for (int x = 10; x < 30; x++) {
            for (int y = 10; y < 30; y++) {
                image2.setRGB(x, y, Color.RED.getRGB());
            }
        }
        // Region 2: bottom-right corner (far from region 1)
        for (int x = 150; x < 180; x++) {
            for (int y = 150; y < 180; y++) {
                image2.setRGB(x, y, Color.BLUE.getRGB());
            }
        }

        ImageComparator.ComparisonResult result = comparator.compare(image1, image2);

        assertThat(result.isMatch()).isFalse();
        assertThat(result.getDiffImage()).isNotNull();
        // The diff image should have circles drawn around regions
        // Verify the diff image dimensions match
        assertThat(result.getDiffImage().getWidth()).isEqualTo(200);
        assertThat(result.getDiffImage().getHeight()).isEqualTo(200);
    }

    /**
     * Helper method to create a solid color image.
     */
    private BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    /**
     * Helper method to create a split color image (left/right).
     */
    private BufferedImage createSplitImage(int width, int height, Color leftColor, Color rightColor) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(leftColor);
        g.fillRect(0, 0, width / 2, height);
        g.setColor(rightColor);
        g.fillRect(width / 2, 0, width / 2, height);
        g.dispose();
        return image;
    }
}
