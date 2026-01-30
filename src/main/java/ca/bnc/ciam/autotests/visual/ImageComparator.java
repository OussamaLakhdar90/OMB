package ca.bnc.ciam.autotests.visual;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Image comparison using OpenCV for visual regression testing.
 * Provides ML-based pixel comparison with configurable tolerance.
 */
@Slf4j
public class ImageComparator {

    private static boolean openCvLoaded = false;

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            openCvLoaded = true;
            log.info("OpenCV loaded successfully");
        } catch (Exception e) {
            log.warn("OpenCV not available. Falling back to Java-based comparison: {}", e.getMessage());
        }
    }

    private final double defaultTolerance;

    /**
     * Create comparator with default tolerance.
     */
    public ImageComparator() {
        this(0.003); // 0.3% tolerance by default - very strict
    }

    /**
     * Create comparator with custom tolerance.
     */
    public ImageComparator(double tolerance) {
        this.defaultTolerance = tolerance;
    }

    /**
     * Compare two images and return comparison result.
     */
    public ComparisonResult compare(BufferedImage baseline, BufferedImage actual) {
        return compare(baseline, actual, defaultTolerance, null);
    }

    /**
     * Compare two images with custom tolerance.
     */
    public ComparisonResult compare(BufferedImage baseline, BufferedImage actual, double tolerance) {
        return compare(baseline, actual, tolerance, null);
    }

    /**
     * Compare two images with ignore regions.
     */
    public ComparisonResult compare(BufferedImage baseline, BufferedImage actual,
                                     double tolerance, List<int[]> ignoreRegions) {
        if (openCvLoaded) {
            return compareWithOpenCV(baseline, actual, tolerance, ignoreRegions);
        } else {
            return compareWithJava(baseline, actual, tolerance, ignoreRegions);
        }
    }

    /**
     * OpenCV-based comparison.
     */
    private ComparisonResult compareWithOpenCV(BufferedImage baseline, BufferedImage actual,
                                                double tolerance, List<int[]> ignoreRegions) {
        try {
            boolean wasScaled = false;
            double scaleFactor = 1.0;
            int originalActualWidth = actual.getWidth();
            int originalActualHeight = actual.getHeight();

            // Check if scaling is needed
            if (baseline.getWidth() != actual.getWidth() || baseline.getHeight() != actual.getHeight()) {
                wasScaled = true;
                scaleFactor = (double) baseline.getWidth() / actual.getWidth();

                log.warn("========================================");
                log.warn("RESOLUTION MISMATCH DETECTED (Local Execution)");
                log.warn("Baseline: {}x{}, Actual: {}x{}", baseline.getWidth(), baseline.getHeight(),
                        actual.getWidth(), actual.getHeight());
                log.warn("Scale factor: {:.2f}x - Actual will be scaled UP to match baseline", scaleFactor);
                log.warn("NOTE: Comparison may have higher tolerance due to scaling artifacts");
                log.warn("========================================");

                // Use high-quality Java scaling before converting to OpenCV
                actual = scaleImageHighQuality(actual, baseline.getWidth(), baseline.getHeight());
            }

            // Convert to OpenCV Mat
            Mat baselineMat = bufferedImageToMat(baseline);
            Mat actualMat = bufferedImageToMat(actual);

            // Apply ignore regions (mask)
            if (ignoreRegions != null && !ignoreRegions.isEmpty()) {
                Mat mask = createIgnoreMask(baselineMat.size(), ignoreRegions);
                Core.bitwise_and(baselineMat, mask, baselineMat);
                Core.bitwise_and(actualMat, mask, actualMat);
            }

            // Calculate absolute difference
            Mat diff = new Mat();
            Core.absdiff(baselineMat, actualMat, diff);

            // Convert to grayscale for analysis
            Mat grayDiff = new Mat();
            if (diff.channels() > 1) {
                Imgproc.cvtColor(diff, grayDiff, Imgproc.COLOR_BGR2GRAY);
            } else {
                grayDiff = diff;
            }

            // Apply threshold to find significant differences
            // Higher threshold (50) to ignore anti-aliasing and minor rendering differences
            Mat thresholded = new Mat();
            Imgproc.threshold(grayDiff, thresholded, 50, 255, Imgproc.THRESH_BINARY);

            // Count non-zero pixels (differences)
            int diffPixels = Core.countNonZero(thresholded);
            int totalPixels = baselineMat.rows() * baselineMat.cols();
            double diffPercentage = (double) diffPixels / totalPixels;

            boolean match = diffPercentage <= tolerance;

            // Create diff image highlighting differences
            BufferedImage diffImage = createDiffImage(baseline, actual, matToBufferedImage(thresholded));

            log.info("Image comparison: diff={}%, tolerance={}%, match={}, scaled={}",
                    String.format("%.4f", diffPercentage * 100),
                    String.format("%.4f", tolerance * 100),
                    match, wasScaled);

            return ComparisonResult.builder()
                    .match(match)
                    .diffPercentage(diffPercentage)
                    .tolerance(tolerance)
                    .diffPixelCount(diffPixels)
                    .totalPixelCount(totalPixels)
                    .diffImage(diffImage)
                    .baselineWidth(baseline.getWidth())
                    .baselineHeight(baseline.getHeight())
                    .actualWidth(originalActualWidth)
                    .actualHeight(originalActualHeight)
                    .wasScaled(wasScaled)
                    .scaleFactor(scaleFactor)
                    .build();

        } catch (Exception e) {
            log.error("OpenCV comparison failed, falling back to Java", e);
            return compareWithJava(baseline, actual, tolerance, ignoreRegions);
        }
    }

    /**
     * Java-based comparison (fallback when OpenCV not available).
     */
    private ComparisonResult compareWithJava(BufferedImage baseline, BufferedImage actual,
                                              double tolerance, List<int[]> ignoreRegions) {
        boolean wasScaled = false;
        double scaleFactor = 1.0;
        int originalActualWidth = actual.getWidth();
        int originalActualHeight = actual.getHeight();

        // Check if scaling is needed
        if (baseline.getWidth() != actual.getWidth() || baseline.getHeight() != actual.getHeight()) {
            wasScaled = true;
            scaleFactor = (double) baseline.getWidth() / actual.getWidth();

            log.warn("========================================");
            log.warn("RESOLUTION MISMATCH DETECTED (Local Execution)");
            log.warn("Baseline: {}x{}, Actual: {}x{}", baseline.getWidth(), baseline.getHeight(),
                    actual.getWidth(), actual.getHeight());
            log.warn("Scale factor: {:.2f}x - Actual will be scaled UP to match baseline", scaleFactor);
            log.warn("========================================");

            // Scale actual to match baseline dimensions using high-quality scaling
            actual = scaleImageHighQuality(actual, baseline.getWidth(), baseline.getHeight());
        }

        int width = baseline.getWidth();
        int height = baseline.getHeight();

        int diffPixels = 0;
        int totalPixels = width * height;

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = diffImage.createGraphics();
        g.drawImage(actual, 0, 0, null);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Check if pixel is in ignore region
                if (isInIgnoreRegion(x, y, ignoreRegions)) {
                    continue;
                }

                int baselineRgb = baseline.getRGB(x, y);
                int actualRgb = actual.getRGB(x, y);

                if (!colorsMatch(baselineRgb, actualRgb)) {
                    diffPixels++;
                    diffImage.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }

        g.dispose();

        double diffPercentage = (double) diffPixels / totalPixels;
        boolean match = diffPercentage <= tolerance;

        log.info("Java image comparison: diff={}%, tolerance={}%, match={}, scaled={}",
                String.format("%.4f", diffPercentage * 100),
                String.format("%.4f", tolerance * 100),
                match, wasScaled);

        return ComparisonResult.builder()
                .match(match)
                .diffPercentage(diffPercentage)
                .tolerance(tolerance)
                .diffPixelCount(diffPixels)
                .totalPixelCount(totalPixels)
                .diffImage(diffImage)
                .baselineWidth(baseline.getWidth())
                .baselineHeight(baseline.getHeight())
                .actualWidth(originalActualWidth)
                .actualHeight(originalActualHeight)
                .wasScaled(wasScaled)
                .scaleFactor(scaleFactor)
                .build();
    }

    /**
     * Check if two colors match (with threshold for anti-aliasing).
     */
    private boolean colorsMatch(int rgb1, int rgb2) {
        int threshold = 50; // Allow differences for anti-aliasing and sub-pixel rendering

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

    /**
     * Check if a pixel is within any ignore region.
     */
    private boolean isInIgnoreRegion(int x, int y, List<int[]> ignoreRegions) {
        if (ignoreRegions == null) {
            return false;
        }
        for (int[] region : ignoreRegions) {
            // region format: [x, y, width, height]
            if (x >= region[0] && x < region[0] + region[2] &&
                    y >= region[1] && y < region[1] + region[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Convert BufferedImage to OpenCV Mat.
     */
    private Mat bufferedImageToMat(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        Mat mat = Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
        return mat;
    }

    /**
     * Convert OpenCV Mat to BufferedImage.
     */
    private BufferedImage matToBufferedImage(Mat mat) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".png", mat, mob);
        return ImageIO.read(new ByteArrayInputStream(mob.toArray()));
    }

    /**
     * Create mask for ignore regions.
     */
    private Mat createIgnoreMask(Size size, List<int[]> ignoreRegions) {
        Mat mask = Mat.ones(size, CvType.CV_8UC3);
        mask.setTo(new Scalar(255, 255, 255));

        for (int[] region : ignoreRegions) {
            Rect rect = new Rect(region[0], region[1], region[2], region[3]);
            Mat roi = mask.submat(rect);
            roi.setTo(new Scalar(0, 0, 0));
        }

        return mask;
    }

    /**
     * Create diff image with prominent visual highlighting.
     * Detects separate diff regions and draws a circle around each one.
     */
    private BufferedImage createDiffImage(BufferedImage baseline, BufferedImage actual, BufferedImage mask) {
        int width = baseline.getWidth();
        int height = baseline.getHeight();

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = diffImage.createGraphics();

        // Draw actual image as base
        g.drawImage(actual, 0, 0, null);

        // Enable anti-aliasing for smooth circles
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                           java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Find separate diff regions using connected component labeling
        List<int[]> diffRegions = findDiffRegions(mask, width, height);

        if (!diffRegions.isEmpty()) {
            g.setColor(new Color(255, 0, 0));
            g.setStroke(new java.awt.BasicStroke(4.0f));

            int regionNum = 1;
            int totalDiffPixels = 0;

            for (int[] region : diffRegions) {
                int minX = region[0];
                int minY = region[1];
                int maxX = region[2];
                int maxY = region[3];
                int pixelCount = region[4];
                totalDiffPixels += pixelCount;

                // Calculate center and dimensions for the ellipse/circle
                int centerX = (minX + maxX) / 2;
                int centerY = (minY + maxY) / 2;
                int regionWidth = maxX - minX;
                int regionHeight = maxY - minY;

                // Add padding around the region (minimum 20 pixels)
                int paddingX = Math.max(20, regionWidth / 4);
                int paddingY = Math.max(20, regionHeight / 4);
                int ellipseWidth = regionWidth + paddingX * 2;
                int ellipseHeight = regionHeight + paddingY * 2;

                // Draw ellipse (circle if square region) around the diff area
                g.setColor(new Color(255, 0, 0));
                g.drawOval(centerX - ellipseWidth / 2, centerY - ellipseHeight / 2,
                           ellipseWidth, ellipseHeight);

                // Add region number label
                g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
                String label = String.format("#%d", regionNum);
                int labelX = centerX - 10;
                int labelY = centerY - ellipseHeight / 2 - 8;
                // Draw label background
                g.setColor(new Color(255, 255, 255, 200));
                g.fillRect(labelX - 2, labelY - 12, 25, 16);
                g.setColor(new Color(255, 0, 0));
                g.drawString(label, labelX, labelY);

                log.info("Diff region #{}: {} pixels at ({},{}) to ({},{})",
                        regionNum, pixelCount, minX, minY, maxX, maxY);
                regionNum++;
            }

            // Add summary at top of image
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            String summary = String.format("DIFF: %d regions, %d pixels total", diffRegions.size(), totalDiffPixels);
            // Draw background for better readability
            g.setColor(new Color(255, 255, 255, 220));
            g.fillRect(8, 8, g.getFontMetrics().stringWidth(summary) + 10, 22);
            g.setColor(new Color(255, 0, 0));
            g.drawString(summary, 12, 24);

            log.info("Total diff: {} regions, {} pixels", diffRegions.size(), totalDiffPixels);
        }

        g.dispose();
        return diffImage;
    }

    /**
     * Find separate diff regions using flood-fill connected component detection.
     * Returns list of bounding boxes: [minX, minY, maxX, maxY, pixelCount]
     */
    private List<int[]> findDiffRegions(BufferedImage mask, int width, int height) {
        List<int[]> regions = new ArrayList<>();
        boolean[][] visited = new boolean[height][width];

        // Minimum region size to filter out noise (at least 10 pixels)
        int minRegionSize = 10;
        // Maximum gap to merge nearby regions (pixels within this distance are considered same region)
        int mergeThreshold = 50;

        for (int y = 0; y < Math.min(height, mask.getHeight()); y++) {
            for (int x = 0; x < Math.min(width, mask.getWidth()); x++) {
                if (!visited[y][x]) {
                    int maskPixel = mask.getRGB(x, y) & 0xff;
                    if (maskPixel > 0) {
                        // Found a diff pixel, flood fill to find the region
                        int[] bounds = floodFillRegion(mask, visited, x, y, width, height);
                        if (bounds[4] >= minRegionSize) {
                            regions.add(bounds);
                        }
                    }
                }
            }
        }

        // Merge nearby regions (regions that are close together)
        regions = mergeNearbyRegions(regions, mergeThreshold);

        return regions;
    }

    /**
     * Flood fill to find connected diff region.
     * Returns [minX, minY, maxX, maxY, pixelCount]
     */
    private int[] floodFillRegion(BufferedImage mask, boolean[][] visited, int startX, int startY, int width, int height) {
        int minX = startX, minY = startY, maxX = startX, maxY = startY;
        int pixelCount = 0;

        // Use iterative approach with stack to avoid stack overflow
        java.util.Deque<int[]> stack = new java.util.ArrayDeque<>();
        stack.push(new int[]{startX, startY});

        // Allow small gaps in the region (connectivity threshold)
        int connectivityRadius = 3;

        while (!stack.isEmpty()) {
            int[] pos = stack.pop();
            int x = pos[0];
            int y = pos[1];

            if (x < 0 || x >= Math.min(width, mask.getWidth()) ||
                y < 0 || y >= Math.min(height, mask.getHeight())) {
                continue;
            }
            if (visited[y][x]) {
                continue;
            }

            int maskPixel = mask.getRGB(x, y) & 0xff;
            if (maskPixel == 0) {
                continue;
            }

            visited[y][x] = true;
            pixelCount++;

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;

            // Check 8-connected neighbors (and slightly beyond for small gaps)
            for (int dy = -connectivityRadius; dy <= connectivityRadius; dy++) {
                for (int dx = -connectivityRadius; dx <= connectivityRadius; dx++) {
                    if (dx != 0 || dy != 0) {
                        stack.push(new int[]{x + dx, y + dy});
                    }
                }
            }
        }

        return new int[]{minX, minY, maxX, maxY, pixelCount};
    }

    /**
     * Merge regions that are close to each other.
     */
    private List<int[]> mergeNearbyRegions(List<int[]> regions, int threshold) {
        if (regions.size() <= 1) {
            return regions;
        }

        List<int[]> merged = new ArrayList<>();
        boolean[] used = new boolean[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            if (used[i]) continue;

            int[] current = regions.get(i).clone();
            used[i] = true;

            // Check if any other region should be merged
            boolean foundMerge;
            do {
                foundMerge = false;
                for (int j = 0; j < regions.size(); j++) {
                    if (used[j]) continue;

                    int[] other = regions.get(j);
                    if (regionsOverlapOrNear(current, other, threshold)) {
                        // Merge: expand current to include other
                        current[0] = Math.min(current[0], other[0]);
                        current[1] = Math.min(current[1], other[1]);
                        current[2] = Math.max(current[2], other[2]);
                        current[3] = Math.max(current[3], other[3]);
                        current[4] += other[4];
                        used[j] = true;
                        foundMerge = true;
                    }
                }
            } while (foundMerge);

            merged.add(current);
        }

        return merged;
    }

    /**
     * Check if two regions overlap or are within threshold distance.
     */
    private boolean regionsOverlapOrNear(int[] r1, int[] r2, int threshold) {
        // Expand r1 by threshold and check overlap
        int r1minX = r1[0] - threshold;
        int r1minY = r1[1] - threshold;
        int r1maxX = r1[2] + threshold;
        int r1maxY = r1[3] + threshold;

        // Check if r2 overlaps with expanded r1
        return !(r2[2] < r1minX || r2[0] > r1maxX || r2[3] < r1minY || r2[1] > r1maxY);
    }

    /**
     * Parse ignore regions from string format "x,y,w,h;x,y,w,h".
     */
    public static List<int[]> parseIgnoreRegions(String regionsString) {
        List<int[]> regions = new ArrayList<>();
        if (regionsString == null || regionsString.isEmpty()) {
            return regions;
        }

        String[] regionStrings = regionsString.split(";");
        for (String regionStr : regionStrings) {
            String[] parts = regionStr.trim().split(",");
            if (parts.length == 4) {
                try {
                    int[] region = new int[]{
                            Integer.parseInt(parts[0].trim()),
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            Integer.parseInt(parts[3].trim())
                    };
                    regions.add(region);
                } catch (NumberFormatException e) {
                    log.warn("Invalid ignore region format: {}", regionStr);
                }
            }
        }

        return regions;
    }

    /**
     * Scale image using high-quality bicubic interpolation.
     * This produces better results than simple nearest-neighbor or bilinear scaling.
     *
     * @param original the original image
     * @param targetWidth target width
     * @param targetHeight target height
     * @return scaled image with high quality
     */
    public static BufferedImage scaleImageHighQuality(BufferedImage original, int targetWidth, int targetHeight) {
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();

        // Use high-quality rendering hints for better scaling
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        return scaled;
    }

    /**
     * Comparison result data class.
     */
    @Data
    @Builder
    public static class ComparisonResult {
        private boolean match;
        private double diffPercentage;
        private double tolerance;
        private int diffPixelCount;
        private int totalPixelCount;
        private BufferedImage diffImage;
        private int baselineWidth;
        private int baselineHeight;
        private int actualWidth;
        private int actualHeight;
        /** True if actual image was scaled to match baseline dimensions */
        @Builder.Default
        private boolean wasScaled = false;
        /** Scale factor applied (1.0 = no scaling, >1.0 = upscaled, <1.0 = downscaled) */
        @Builder.Default
        private double scaleFactor = 1.0;

        public String getSummary() {
            String scalingInfo = wasScaled ? String.format(", Scaled: %.2fx", scaleFactor) : "";
            return String.format("Match: %s, Diff: %.4f%%, Pixels: %d/%d, Tolerance: %.4f%%%s",
                    match, diffPercentage * 100, diffPixelCount, totalPixelCount, tolerance * 100, scalingInfo);
        }
    }
}
