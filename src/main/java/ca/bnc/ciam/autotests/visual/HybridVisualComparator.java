package ca.bnc.ciam.autotests.visual;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Hybrid image comparator that combines fast pixel-based comparison with AI fallback.
 *
 * Strategy:
 * 1. First, run fast pixel-based comparison (OpenCV/Java)
 * 2. If result is in the "gray zone" (configurable), use AI for final decision
 * 3. AI uses ResNet18 feature extraction + cosine similarity
 *
 * Gray Zone (default 80%-95% similarity):
 * - Below 80%: Definitely different, no AI needed
 * - Above 95%: Definitely match, no AI needed
 * - Between 80%-95%: Uncertain, use AI to decide
 *
 * Benefits:
 * - Fast for clear pass/fail cases
 * - AI handles edge cases (anti-aliasing, font rendering, minor layout shifts)
 * - Reduces false positives in visual testing
 *
 * Usage:
 * <pre>
 * HybridVisualComparator comparator = new HybridVisualComparator();
 * HybridComparisonResult result = comparator.compare(baseline, actual, 0.01);
 * if (result.isMatch()) {
 *     // Images match according to hybrid strategy
 * }
 * </pre>
 */
@Slf4j
public class HybridVisualComparator implements AutoCloseable {

    // Gray zone boundaries (pixel diff percentage)
    private static final double DEFAULT_GRAY_ZONE_LOWER = 0.05;  // 5% diff = 95% match
    private static final double DEFAULT_GRAY_ZONE_UPPER = 0.20;  // 20% diff = 80% match

    // AI similarity threshold for gray zone decisions
    private static final double DEFAULT_AI_THRESHOLD = 0.92;

    private final ImageComparator pixelComparator;
    private final AIImageComparator aiComparator;
    private final double grayZoneLower;
    private final double grayZoneUpper;
    private final double aiThreshold;
    private final boolean aiEnabled;

    /**
     * Create hybrid comparator with default settings.
     * AI is enabled by default but gracefully degrades if unavailable.
     */
    public HybridVisualComparator() {
        this(0.01, DEFAULT_GRAY_ZONE_LOWER, DEFAULT_GRAY_ZONE_UPPER, DEFAULT_AI_THRESHOLD, true);
    }

    /**
     * Create hybrid comparator with custom tolerance.
     */
    public HybridVisualComparator(double tolerance) {
        this(tolerance, DEFAULT_GRAY_ZONE_LOWER, DEFAULT_GRAY_ZONE_UPPER, DEFAULT_AI_THRESHOLD, true);
    }

    /**
     * Create hybrid comparator with full configuration.
     *
     * @param tolerance      pixel comparison tolerance
     * @param grayZoneLower  lower boundary of gray zone (diff percentage)
     * @param grayZoneUpper  upper boundary of gray zone (diff percentage)
     * @param aiThreshold    AI similarity threshold for match decision
     * @param enableAI       whether to enable AI fallback
     */
    public HybridVisualComparator(double tolerance, double grayZoneLower, double grayZoneUpper,
                                   double aiThreshold, boolean enableAI) {
        this.pixelComparator = new ImageComparator(tolerance);
        this.grayZoneLower = grayZoneLower;
        this.grayZoneUpper = grayZoneUpper;
        this.aiThreshold = aiThreshold;

        if (enableAI) {
            AIImageComparator tempAI = new AIImageComparator(aiThreshold);
            if (tempAI.isAvailable()) {
                this.aiComparator = tempAI;
                this.aiEnabled = true;
                log.info("Hybrid comparator initialized with AI support");
            } else {
                log.warn("AI comparator not available: {}. Using pixel-based comparison only.",
                        tempAI.getInitError());
                tempAI.close();
                this.aiComparator = null;
                this.aiEnabled = false;
            }
        } else {
            this.aiComparator = null;
            this.aiEnabled = false;
            log.info("Hybrid comparator initialized without AI (disabled)");
        }
    }

    /**
     * Check if AI comparison is available.
     */
    public boolean isAIAvailable() {
        return aiEnabled && aiComparator != null && aiComparator.isAvailable();
    }

    /**
     * Compare two images using hybrid strategy.
     */
    public HybridComparisonResult compare(BufferedImage baseline, BufferedImage actual) {
        return compare(baseline, actual, pixelComparator.compare(baseline, actual).getTolerance(), null);
    }

    /**
     * Compare two images with custom tolerance.
     */
    public HybridComparisonResult compare(BufferedImage baseline, BufferedImage actual, double tolerance) {
        return compare(baseline, actual, tolerance, null);
    }

    /**
     * Compare two images with ignore regions.
     */
    public HybridComparisonResult compare(BufferedImage baseline, BufferedImage actual,
                                           double tolerance, List<int[]> ignoreRegions) {
        long startTime = System.currentTimeMillis();

        // Step 1: Fast pixel-based comparison
        ImageComparator.ComparisonResult pixelResult = pixelComparator.compare(baseline, actual, tolerance, ignoreRegions);
        double diffPercentage = pixelResult.getDiffPercentage();

        log.debug("Pixel comparison: diff={}%, tolerance={}%",
                String.format("%.4f", diffPercentage * 100),
                String.format("%.4f", tolerance * 100));

        // Step 2: Determine if we need AI fallback
        ComparisonStrategy strategy;
        boolean finalMatch;
        AIImageComparator.AIComparisonResult aiResult = null;

        if (diffPercentage <= tolerance) {
            // Clear PASS: below tolerance, no AI needed
            strategy = ComparisonStrategy.PIXEL_PASS;
            finalMatch = true;
            log.debug("Clear PASS: diff {} <= tolerance {}", diffPercentage, tolerance);

        } else if (diffPercentage > grayZoneUpper) {
            // Clear FAIL: above gray zone upper bound, too different for AI to help
            strategy = ComparisonStrategy.PIXEL_FAIL;
            finalMatch = false;
            log.debug("Clear FAIL: diff {} > gray zone upper {}", diffPercentage, grayZoneUpper);

        } else if (diffPercentage > grayZoneLower && diffPercentage <= grayZoneUpper && isAIAvailable()) {
            // GRAY ZONE: Use AI to decide
            strategy = ComparisonStrategy.AI_FALLBACK;
            log.info("Gray zone detected (diff={}%), using AI fallback", String.format("%.4f", diffPercentage * 100));

            aiResult = aiComparator.compare(baseline, actual, aiThreshold);
            finalMatch = aiResult.isMatch();

            if (finalMatch) {
                log.info("AI says MATCH with similarity {}", String.format("%.4f", aiResult.getSimilarity()));
            } else {
                log.info("AI says NO MATCH with similarity {}", String.format("%.4f", aiResult.getSimilarity()));
            }

        } else {
            // Gray zone but AI not available - use pixel result
            strategy = ComparisonStrategy.PIXEL_ONLY;
            finalMatch = pixelResult.isMatch();
            log.debug("Gray zone but AI unavailable, using pixel result: {}", finalMatch);
        }

        long duration = System.currentTimeMillis() - startTime;

        return HybridComparisonResult.builder()
                .match(finalMatch)
                .strategy(strategy)
                .pixelResult(pixelResult)
                .aiResult(aiResult)
                .diffPercentage(diffPercentage)
                .tolerance(tolerance)
                .grayZoneLower(grayZoneLower)
                .grayZoneUpper(grayZoneUpper)
                .aiThreshold(aiThreshold)
                .comparisonTimeMs(duration)
                .build();
    }

    @Override
    public void close() {
        if (aiComparator != null) {
            aiComparator.close();
        }
    }

    /**
     * Strategy used for final comparison decision.
     */
    public enum ComparisonStrategy {
        /** Pixel comparison passed (below tolerance) */
        PIXEL_PASS,
        /** Pixel comparison failed (above gray zone) */
        PIXEL_FAIL,
        /** Pixel comparison in gray zone, AI made final decision */
        AI_FALLBACK,
        /** Pixel comparison only (AI not available) */
        PIXEL_ONLY
    }

    /**
     * Hybrid comparison result data class.
     */
    @Data
    @Builder
    public static class HybridComparisonResult {
        private boolean match;
        private ComparisonStrategy strategy;
        private ImageComparator.ComparisonResult pixelResult;
        private AIImageComparator.AIComparisonResult aiResult;
        private double diffPercentage;
        private double tolerance;
        private double grayZoneLower;
        private double grayZoneUpper;
        private double aiThreshold;
        private long comparisonTimeMs;

        /**
         * Check if AI was used for this comparison.
         */
        public boolean usedAI() {
            return strategy == ComparisonStrategy.AI_FALLBACK && aiResult != null;
        }

        /**
         * Get the diff image (from pixel comparison).
         */
        public BufferedImage getDiffImage() {
            return pixelResult != null ? pixelResult.getDiffImage() : null;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Hybrid Match: %s, Strategy: %s, Diff: %.4f%%, Tolerance: %.4f%%",
                    match, strategy, diffPercentage * 100, tolerance * 100));

            if (usedAI()) {
                sb.append(String.format(", AI Similarity: %.4f", aiResult.getSimilarity()));
            }

            sb.append(String.format(", Time: %dms", comparisonTimeMs));
            return sb.toString();
        }
    }
}
