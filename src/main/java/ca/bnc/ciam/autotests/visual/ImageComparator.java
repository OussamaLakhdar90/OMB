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
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
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
        this(0.01); // 1% tolerance by default
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
            // Convert to OpenCV Mat
            Mat baselineMat = bufferedImageToMat(baseline);
            Mat actualMat = bufferedImageToMat(actual);

            // Resize if dimensions don't match
            if (baselineMat.rows() != actualMat.rows() || baselineMat.cols() != actualMat.cols()) {
                log.warn("Image dimensions differ. Baseline: {}x{}, Actual: {}x{}. Resizing actual.",
                        baselineMat.cols(), baselineMat.rows(), actualMat.cols(), actualMat.rows());
                Imgproc.resize(actualMat, actualMat, new Size(baselineMat.cols(), baselineMat.rows()));
            }

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
            Mat thresholded = new Mat();
            Imgproc.threshold(grayDiff, thresholded, 25, 255, Imgproc.THRESH_BINARY);

            // Count non-zero pixels (differences)
            int diffPixels = Core.countNonZero(thresholded);
            int totalPixels = baselineMat.rows() * baselineMat.cols();
            double diffPercentage = (double) diffPixels / totalPixels;

            boolean match = diffPercentage <= tolerance;

            // Create diff image highlighting differences
            BufferedImage diffImage = createDiffImage(baseline, actual, matToBufferedImage(thresholded));

            log.info("Image comparison: diff={}%, tolerance={}%, match={}",
                    String.format("%.4f", diffPercentage * 100),
                    String.format("%.4f", tolerance * 100),
                    match);

            return ComparisonResult.builder()
                    .match(match)
                    .diffPercentage(diffPercentage)
                    .tolerance(tolerance)
                    .diffPixelCount(diffPixels)
                    .totalPixelCount(totalPixels)
                    .diffImage(diffImage)
                    .baselineWidth(baseline.getWidth())
                    .baselineHeight(baseline.getHeight())
                    .actualWidth(actual.getWidth())
                    .actualHeight(actual.getHeight())
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
        int width = Math.min(baseline.getWidth(), actual.getWidth());
        int height = Math.min(baseline.getHeight(), actual.getHeight());

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

        log.info("Java image comparison: diff={}%, tolerance={}%, match={}",
                String.format("%.4f", diffPercentage * 100),
                String.format("%.4f", tolerance * 100),
                match);

        return ComparisonResult.builder()
                .match(match)
                .diffPercentage(diffPercentage)
                .tolerance(tolerance)
                .diffPixelCount(diffPixels)
                .totalPixelCount(totalPixels)
                .diffImage(diffImage)
                .baselineWidth(baseline.getWidth())
                .baselineHeight(baseline.getHeight())
                .actualWidth(actual.getWidth())
                .actualHeight(actual.getHeight())
                .build();
    }

    /**
     * Check if two colors match (with threshold for anti-aliasing).
     */
    private boolean colorsMatch(int rgb1, int rgb2) {
        int threshold = 25; // Allow small differences for anti-aliasing

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
     * Create diff image highlighting differences.
     */
    private BufferedImage createDiffImage(BufferedImage baseline, BufferedImage actual, BufferedImage mask) {
        int width = baseline.getWidth();
        int height = baseline.getHeight();

        BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = diffImage.createGraphics();

        // Draw actual image
        g.drawImage(actual, 0, 0, null);

        // Overlay differences in red
        g.setColor(new Color(255, 0, 0, 128));
        for (int y = 0; y < Math.min(height, mask.getHeight()); y++) {
            for (int x = 0; x < Math.min(width, mask.getWidth()); x++) {
                int maskPixel = mask.getRGB(x, y) & 0xff;
                if (maskPixel > 0) {
                    g.fillRect(x, y, 1, 1);
                }
            }
        }

        g.dispose();
        return diffImage;
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

        public String getSummary() {
            return String.format("Match: %s, Diff: %.4f%%, Pixels: %d/%d, Tolerance: %.4f%%",
                    match, diffPercentage * 100, diffPixelCount, totalPixelCount, tolerance * 100);
        }
    }
}
