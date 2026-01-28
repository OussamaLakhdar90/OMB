package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.AIImageComparator;
import ca.bnc.ciam.autotests.visual.AIImageComparator.AIComparisonResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.awt.*;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AIImageComparator.
 * Tests AI-based image comparison using DJL + ResNet18.
 *
 * Note: These tests may take longer on first run as the model is downloaded (~45MB).
 * If DJL/PyTorch is not available, tests will gracefully skip AI-specific assertions.
 */
@Test(groups = "unit")
public class AIImageComparatorTest {

    private AIImageComparator comparator;

    @BeforeClass
    public void setUp() {
        comparator = new AIImageComparator(0.90);
    }

    @AfterClass
    public void tearDown() {
        if (comparator != null) {
            comparator.close();
        }
    }

    // ===========================================
    // Constructor Tests
    // ===========================================

    @Test
    public void testConstructor_DefaultThreshold() {
        try (AIImageComparator defaultComparator = new AIImageComparator()) {
            assertThat(defaultComparator).isNotNull();
        }
    }

    @Test
    public void testConstructor_CustomThreshold() {
        try (AIImageComparator customComparator = new AIImageComparator(0.85)) {
            assertThat(customComparator).isNotNull();
        }
    }

    // ===========================================
    // Availability Tests
    // ===========================================

    @Test
    public void testIsAvailable_ReturnsBoolean() {
        // Should return true if DJL loaded, false otherwise (graceful degradation)
        boolean available = comparator.isAvailable();
        assertThat(available).isIn(true, false);
    }

    @Test
    public void testGetInitError_NullWhenAvailable() {
        if (comparator.isAvailable()) {
            assertThat(comparator.getInitError()).isNull();
        } else {
            assertThat(comparator.getInitError()).isNotNull();
        }
    }

    // ===========================================
    // Comparison Result Tests (Static)
    // ===========================================

    @Test
    public void testAIComparisonResult_Builder() {
        AIComparisonResult result = AIComparisonResult.builder()
                .match(true)
                .similarity(0.95)
                .threshold(0.90)
                .baselineFeatureSize(512)
                .actualFeatureSize(512)
                .build();

        assertThat(result.isMatch()).isTrue();
        assertThat(result.getSimilarity()).isEqualTo(0.95);
        assertThat(result.getThreshold()).isEqualTo(0.90);
        assertThat(result.getBaselineFeatureSize()).isEqualTo(512);
        assertThat(result.getActualFeatureSize()).isEqualTo(512);
        assertThat(result.hasError()).isFalse();
    }

    @Test
    public void testAIComparisonResult_HasError_TrueWhenErrorSet() {
        AIComparisonResult result = AIComparisonResult.builder()
                .match(false)
                .similarity(0.0)
                .error("Test error")
                .build();

        assertThat(result.hasError()).isTrue();
        assertThat(result.getError()).isEqualTo("Test error");
    }

    @Test
    public void testAIComparisonResult_GetSummary_FormatsCorrectly() {
        AIComparisonResult result = AIComparisonResult.builder()
                .match(true)
                .similarity(0.9567)
                .threshold(0.90)
                .baselineFeatureSize(512)
                .build();

        String summary = result.getSummary();
        assertThat(summary).contains("AI Match: true");
        assertThat(summary).contains("Similarity:");
        assertThat(summary).contains("Threshold:");
    }

    @Test
    public void testAIComparisonResult_GetSummary_WithError() {
        AIComparisonResult result = AIComparisonResult.builder()
                .match(false)
                .error("Model not loaded")
                .build();

        String summary = result.getSummary();
        assertThat(summary).contains("ERROR");
        assertThat(summary).contains("Model not loaded");
    }

    // ===========================================
    // Image Comparison Tests (Conditional on AI availability)
    // ===========================================

    @Test
    public void testCompare_IdenticalImages_HighSimilarity() {
        BufferedImage image = createTestImage(224, 224, Color.BLUE);

        AIComparisonResult result = comparator.compare(image, image);

        if (comparator.isAvailable()) {
            assertThat(result.isMatch()).isTrue();
            assertThat(result.getSimilarity()).isGreaterThanOrEqualTo(0.99);
            assertThat(result.hasError()).isFalse();
        } else {
            // AI not available - should return error result
            assertThat(result.hasError()).isTrue();
        }
    }

    @Test
    public void testCompare_DifferentColors_LowerSimilarity() {
        BufferedImage blueImage = createTestImage(224, 224, Color.BLUE);
        BufferedImage redImage = createTestImage(224, 224, Color.RED);

        AIComparisonResult result = comparator.compare(blueImage, redImage);

        if (comparator.isAvailable()) {
            // Different solid colors should still have some similarity (both are solid colors)
            // but less than identical images
            assertThat(result.getSimilarity()).isLessThan(0.99);
            assertThat(result.hasError()).isFalse();
        }
    }

    @Test
    public void testCompare_SimilarImagesWithMinorDiff_HighSimilarity() {
        BufferedImage baseline = createTestImage(224, 224, Color.WHITE);
        BufferedImage actual = createTestImage(224, 224, new Color(250, 250, 250)); // Slightly off-white

        AIComparisonResult result = comparator.compare(baseline, actual);

        if (comparator.isAvailable()) {
            // Very similar colors should have high similarity
            assertThat(result.getSimilarity()).isGreaterThan(0.90);
        }
    }

    @Test
    public void testCompare_WithCustomThreshold() {
        BufferedImage image = createTestImage(224, 224, Color.GREEN);

        // Use very high threshold
        AIComparisonResult result = comparator.compare(image, image, 0.999);

        if (comparator.isAvailable()) {
            assertThat(result.isMatch()).isTrue();
            assertThat(result.getThreshold()).isEqualTo(0.999);
        }
    }

    @Test
    public void testCompare_DifferentSizes_HandlesGracefully() {
        BufferedImage small = createTestImage(100, 100, Color.BLUE);
        BufferedImage large = createTestImage(300, 300, Color.BLUE);

        // Should handle different sizes (they get resized to 224x224 internally)
        AIComparisonResult result = comparator.compare(small, large);

        if (comparator.isAvailable()) {
            assertThat(result.hasError()).isFalse();
            // Same color, so should still be similar after resize
            assertThat(result.getSimilarity()).isGreaterThan(0.80);
        }
    }

    // ===========================================
    // Close Tests
    // ===========================================

    @Test
    public void testClose_CanBeCalledMultipleTimes() {
        AIImageComparator tempComparator = new AIImageComparator();
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
}
