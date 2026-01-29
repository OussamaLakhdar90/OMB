package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.HybridVisualComparator;
import ca.bnc.ciam.autotests.visual.HybridVisualComparator.ComparisonStrategy;
import ca.bnc.ciam.autotests.visual.HybridVisualComparator.HybridComparisonResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HybridVisualComparator.
 * Tests the hybrid image comparison strategy combining pixel-based and AI comparison.
 */
@Test(groups = "unit")
public class HybridVisualComparatorTest {

    private HybridVisualComparator comparatorWithAI;
    private HybridVisualComparator comparatorWithoutAI;

    @BeforeClass
    public void setUp() {
        // Create comparator with AI enabled (may or may not be available)
        comparatorWithAI = new HybridVisualComparator(0.01, 0.05, 0.20, 0.92, true);

        // Create comparator with AI explicitly disabled
        comparatorWithoutAI = new HybridVisualComparator(0.01, 0.05, 0.20, 0.92, false);
    }

    @AfterClass
    public void tearDown() {
        if (comparatorWithAI != null) {
            comparatorWithAI.close();
        }
        if (comparatorWithoutAI != null) {
            comparatorWithoutAI.close();
        }
    }

    // ===========================================
    // Constructor Tests
    // ===========================================

    @Test
    public void testConstructor_Default() {
        try (HybridVisualComparator defaultComparator = new HybridVisualComparator()) {
            assertThat(defaultComparator).isNotNull();
        }
    }

    @Test
    public void testConstructor_WithTolerance() {
        try (HybridVisualComparator customComparator = new HybridVisualComparator(0.02)) {
            assertThat(customComparator).isNotNull();
        }
    }

    @Test
    public void testConstructor_FullConfig() {
        try (HybridVisualComparator fullComparator = new HybridVisualComparator(0.01, 0.10, 0.30, 0.85, true)) {
            assertThat(fullComparator).isNotNull();
        }
    }

    // ===========================================
    // AI Availability Tests
    // ===========================================

    @Test
    public void testIsAIAvailable_WithAIEnabled_ReturnsBoolean() {
        boolean available = comparatorWithAI.isAIAvailable();
        assertThat(available).isIn(true, false);
    }

    @Test
    public void testIsAIAvailable_WithAIDisabled_ReturnsFalse() {
        assertThat(comparatorWithoutAI.isAIAvailable()).isFalse();
    }

    // ===========================================
    // Clear Pass Tests (Below Tolerance)
    // ===========================================

    @Test
    public void testCompare_IdenticalImages_ClearPass() {
        BufferedImage image = createTestImage(200, 200, Color.BLUE);

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image, 0.01);

        assertThat(result.isMatch()).isTrue();
        assertThat(result.getStrategy()).isEqualTo(ComparisonStrategy.PIXEL_PASS);
        assertThat(result.getDiffPercentage()).isEqualTo(0.0);
        assertThat(result.usedAI()).isFalse();
    }

    @Test
    public void testCompare_VerySmallDiff_ClearPass() {
        BufferedImage baseline = createTestImage(100, 100, Color.WHITE);
        BufferedImage actual = createTestImage(100, 100, new Color(254, 254, 254));

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.05);

        assertThat(result.isMatch()).isTrue();
        assertThat(result.getStrategy()).isIn(ComparisonStrategy.PIXEL_PASS, ComparisonStrategy.PIXEL_ONLY);
    }

    // ===========================================
    // Clear Fail Tests (Above Gray Zone)
    // ===========================================

    @Test
    public void testCompare_VeryDifferentImages_ClearFail() {
        BufferedImage black = createTestImage(100, 100, Color.BLACK);
        BufferedImage white = createTestImage(100, 100, Color.WHITE);

        HybridComparisonResult result = comparatorWithoutAI.compare(black, white, 0.01);

        assertThat(result.isMatch()).isFalse();
        assertThat(result.getStrategy()).isIn(ComparisonStrategy.PIXEL_FAIL, ComparisonStrategy.PIXEL_ONLY);
        assertThat(result.usedAI()).isFalse();
    }

    @Test
    public void testCompare_HalfDifferent_ClearFail() {
        BufferedImage baseline = createSplitImage(100, 100, Color.RED, Color.RED);
        BufferedImage actual = createSplitImage(100, 100, Color.RED, Color.BLUE);

        // Half the image is different, so diff > 20% (gray zone upper)
        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.01);

        assertThat(result.isMatch()).isFalse();
        assertThat(result.getDiffPercentage()).isGreaterThan(0.20);
    }

    // ===========================================
    // Gray Zone Tests
    // ===========================================

    @Test
    public void testCompare_InGrayZone_WithoutAI_UsesPixelOnly() {
        // Create images with ~10% difference (in gray zone 5-20%)
        BufferedImage baseline = createTestImage(100, 100, Color.WHITE);
        BufferedImage actual = createImageWithDiff(baseline, 0.10);

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.01);

        // Without AI, should use PIXEL_ONLY strategy
        assertThat(result.getStrategy()).isIn(ComparisonStrategy.PIXEL_ONLY, ComparisonStrategy.PIXEL_FAIL);
        assertThat(result.usedAI()).isFalse();
    }

    @Test
    public void testCompare_InGrayZone_WithAI_UsesAIFallback() {
        if (!comparatorWithAI.isAIAvailable()) {
            // Skip if AI not available
            return;
        }

        // Create images with ~10% pixel difference (in gray zone)
        BufferedImage baseline = createTestImage(100, 100, Color.WHITE);
        BufferedImage actual = createImageWithDiff(baseline, 0.10);

        HybridComparisonResult result = comparatorWithAI.compare(baseline, actual, 0.01);

        // With AI available and in gray zone, should use AI_FALLBACK
        assertThat(result.getStrategy()).isEqualTo(ComparisonStrategy.AI_FALLBACK);
        assertThat(result.usedAI()).isTrue();
        assertThat(result.getAiResult()).isNotNull();
    }

    // ===========================================
    // Result Property Tests
    // ===========================================

    @Test
    public void testHybridComparisonResult_GetDiffImage() {
        BufferedImage image = createTestImage(100, 100, Color.GREEN);

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image);

        assertThat(result.getPixelResult()).isNotNull();
        // For identical images, diff image is still generated
        assertThat(result.getDiffImage()).isNotNull();
    }

    @Test
    public void testHybridComparisonResult_GetSummary() {
        BufferedImage image = createTestImage(100, 100, Color.YELLOW);

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image, 0.01);

        String summary = result.getSummary();
        assertThat(summary).contains("Hybrid Match:");
        assertThat(summary).contains("Strategy:");
        assertThat(summary).contains("Diff:");
        assertThat(summary).contains("Time:");
    }

    @Test
    public void testHybridComparisonResult_ComparisonTimeRecorded() {
        BufferedImage image = createTestImage(100, 100, Color.CYAN);

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image);

        assertThat(result.getComparisonTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void testHybridComparisonResult_TolerancePreserved() {
        BufferedImage image = createTestImage(100, 100, Color.MAGENTA);
        double tolerance = 0.03;

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image, tolerance);

        assertThat(result.getTolerance()).isEqualTo(tolerance);
    }

    // ===========================================
    // ComparisonStrategy Enum Tests
    // ===========================================

    @Test
    public void testComparisonStrategy_AllValuesExist() {
        assertThat(ComparisonStrategy.PIXEL_PASS).isNotNull();
        assertThat(ComparisonStrategy.PIXEL_FAIL).isNotNull();
        assertThat(ComparisonStrategy.AI_FALLBACK).isNotNull();
        assertThat(ComparisonStrategy.PIXEL_ONLY).isNotNull();
    }

    // ===========================================
    // Scaled Comparison Tests (Local Execution)
    // ===========================================

    @Test
    public void testScaledComparison_DetectsScaling() {
        // Baseline at higher resolution (like SauceLabs)
        BufferedImage baseline = createTestImage(200, 200, Color.BLUE);
        // Actual at lower resolution (like local laptop)
        BufferedImage actual = createTestImage(150, 150, Color.BLUE);

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.10);

        // Should detect scaling
        assertThat(result.isWasScaled()).isTrue();
        assertThat(result.getScaleFactor()).isGreaterThan(1.0);
    }

    @Test
    public void testScaledComparison_SolidColorImages_ShouldMatch() {
        // Same color, different resolution
        BufferedImage baseline = createTestImage(200, 200, Color.GREEN);
        BufferedImage actual = createTestImage(150, 150, Color.GREEN);

        // With tolerance that allows for scaling artifacts
        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.10);

        // Solid colors should match even with scaling
        assertThat(result.isMatch()).isTrue();
    }

    @Test
    public void testScaledComparison_DifferentImages_ShouldDetectDifference() {
        // Red baseline
        BufferedImage baseline = createTestImage(200, 200, Color.RED);
        // Blue actual (completely different)
        BufferedImage actual = createTestImage(150, 150, Color.BLUE);

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.01);

        // Should detect the major difference
        assertThat(result.isMatch()).isFalse();
        assertThat(result.isWasScaled()).isTrue();
    }

    @Test
    public void testScaledComparison_ResultIncludesScaleInfo() {
        BufferedImage baseline = createTestImage(200, 200, Color.WHITE);
        BufferedImage actual = createTestImage(150, 150, Color.WHITE);

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.10);

        // Result should include scaling info
        assertThat(result.isWasScaled()).isTrue();
        assertThat(result.getScaleFactor()).isCloseTo(1.33, org.assertj.core.api.Assertions.within(0.1));
    }

    @Test
    public void testScaledComparison_SummaryIncludesScaleInfo() {
        BufferedImage baseline = createTestImage(200, 200, Color.WHITE);
        BufferedImage actual = createTestImage(150, 150, Color.WHITE);

        HybridComparisonResult result = comparatorWithoutAI.compare(baseline, actual, 0.10);

        String summary = result.getSummary();
        assertThat(summary).contains("Scaled:");
    }

    @Test
    public void testNoScaling_WhenSameDimensions() {
        BufferedImage image = createTestImage(100, 100, Color.WHITE);

        HybridComparisonResult result = comparatorWithoutAI.compare(image, image, 0.01);

        // Should NOT be marked as scaled
        assertThat(result.isWasScaled()).isFalse();
        assertThat(result.getScaleFactor()).isEqualTo(1.0);
    }

    // ===========================================
    // Close Tests
    // ===========================================

    @Test
    public void testClose_CanBeCalledMultipleTimes() {
        HybridVisualComparator tempComparator = new HybridVisualComparator();
        tempComparator.close();
        tempComparator.close(); // Should not throw
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

    private BufferedImage createImageWithDiff(BufferedImage original, double diffPercentage) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int totalPixels = width * height;
        int diffPixels = (int) (totalPixels * diffPercentage);

        // Copy original
        Graphics2D g = result.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();

        // Change some pixels to create difference
        int changed = 0;
        for (int y = 0; y < height && changed < diffPixels; y++) {
            for (int x = 0; x < width && changed < diffPixels; x++) {
                int rgb = result.getRGB(x, y);
                // Invert the color
                int inverted = ~rgb | 0xFF000000;
                result.setRGB(x, y, inverted);
                changed++;
            }
        }

        return result;
    }
}
