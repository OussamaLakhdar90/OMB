package ca.bnc.ciam.autotests.utils;

import ca.bnc.ciam.autotests.metrics.MetricsCollector;
import ca.bnc.ciam.autotests.visual.HybridVisualComparator;
import ca.bnc.ciam.autotests.visual.ScreenshotManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Visual capture utility for screenshot recording and comparison.
 *
 * Features:
 * - Fixed resolution: 1920x1080 for consistency
 * - Dynamic scrolling: automatically captures multiple screenshots for long pages
 * - Browser-specific baselines: baselines/{browser}/{class}/{step}_1.png, _2.png, etc.
 * - Clear error messages: distinguishes between structure changes and visual differences
 * - Hybrid comparison: pixel-based with optional AI fallback
 *
 * Usage:
 * <pre>
 * // In debug_config.json: { "record": true }  → Record mode (creates baselines)
 * // In debug_config.json: { "record": false } → Compare mode (validates against baselines)
 *
 * boolean passed = VisualCapture.captureStep(driver, "LoginTest", "login_page");
 * assertThat(passed).as("Visual check failed").isTrue();
 * </pre>
 *
 * File naming:
 * - Single viewport: baselines/{browser}/{class}/{step}_1.png
 * - Multiple viewports: baselines/{browser}/{class}/{step}_1.png, {step}_2.png, etc.
 *
 * System properties:
 * - bnc.record.mode: true/false - Enable record mode to create baselines
 * - bnc.baselines.root: path - Override baseline location
 * - bnc.visual.ai.enabled: true/false - Enable/disable AI fallback (default: true)
 */
@Slf4j
public final class VisualCapture {

    private static final String BASELINES_DIR_NAME = "baselines";
    private static final String REPORT_VISUAL_DIR_NAME = "target/metrics/visual";
    private static final String RECORD_MODE_PROPERTY = "bnc.record.mode";
    private static final String BASELINES_ROOT_PROPERTY = "bnc.baselines.root";
    private static final String AI_ENABLED_PROPERTY = "bnc.visual.ai.enabled";
    private static final double DEFAULT_TOLERANCE = 0.01; // 1%

    private static final Path PROJECT_ROOT = detectProjectRoot();
    private static final ScreenshotManager screenshotManager = new ScreenshotManager();

    // Lazy-loaded comparator to avoid class loading issues when visual testing is not used
    private static volatile HybridVisualComparator hybridComparator;

    /**
     * Get or create the hybrid comparator (lazy initialization).
     */
    private static HybridVisualComparator getHybridComparator() {
        if (hybridComparator == null) {
            synchronized (VisualCapture.class) {
                if (hybridComparator == null) {
                    boolean aiEnabled = !"false".equalsIgnoreCase(System.getProperty(AI_ENABLED_PROPERTY));
                    log.info("Creating hybrid visual comparator with AI enabled: {}", aiEnabled);
                    hybridComparator = new HybridVisualComparator(DEFAULT_TOLERANCE, 0.05, 0.20, 0.92, aiEnabled);
                }
            }
        }
        return hybridComparator;
    }

    /**
     * Stores the last diff image as Base64 for report embedding.
     */
    private static final ThreadLocal<String> lastDiffBase64 = new ThreadLocal<>();

    /**
     * Stores the last error message for reporting.
     */
    private static final ThreadLocal<String> lastErrorMessage = new ThreadLocal<>();

    private VisualCapture() {
        // Utility class - prevent instantiation
    }

    /**
     * Capture a visual step with automatic scrolling support.
     *
     * Recording mode:
     *   - Sets window to 1920x1080
     *   - Calculates how many screenshots needed
     *   - Saves: step_1.png, step_2.png, etc.
     *
     * Comparison mode:
     *   - Sets window to 1920x1080
     *   - Counts baseline files
     *   - Calculates current page needs
     *   - If counts differ → FAIL with clear message
     *   - If counts match → Compare each pair
     *
     * @param driver    the WebDriver instance
     * @param className the test class name (used for folder)
     * @param stepName  the step name (used for file prefix)
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName) {
        return captureStep(driver, className, stepName, DEFAULT_TOLERANCE);
    }

    /**
     * Capture a visual step with custom tolerance.
     *
     * @param driver    the WebDriver instance
     * @param className the test class name (used for folder)
     * @param stepName  the step name (used for file prefix)
     * @param tolerance the comparison tolerance (0.0 to 1.0)
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName, double tolerance) {
        boolean isRecordMode = isRecordMode();
        long startTime = System.currentTimeMillis();

        // Clear previous state
        lastDiffBase64.remove();
        lastErrorMessage.remove();

        // Get browser-specific baseline directory
        String browserName = detectBrowserName(driver);
        Path baselineDir = getBaselineDir(browserName, className);

        log.info("========================================");
        log.info("Visual Capture: {}/{}", className, stepName);
        log.info("Browser: {}", browserName);
        log.info("Mode: {}", isRecordMode ? "RECORD" : "COMPARE");
        log.info("Baseline directory: {}", baselineDir);
        log.info("========================================");

        try {
            // Set standard resolution for consistency
            Dimension originalSize = screenshotManager.ensureStandardResolution(driver);
            log.info("Window size set to {}x{}", ScreenshotManager.DEFAULT_WIDTH, ScreenshotManager.DEFAULT_HEIGHT);

            boolean result;
            if (isRecordMode) {
                result = recordBaselines(driver, baselineDir, className, stepName, startTime);
            } else {
                result = compareWithBaselines(driver, baselineDir, className, stepName, tolerance, startTime);
            }

            // Restore original window size
            screenshotManager.restoreWindowSize(driver, originalSize);

            return result;

        } catch (Exception e) {
            String errorMsg = "Visual capture failed: " + e.getMessage();
            log.error("========================================");
            log.error("VISUAL CAPTURE ERROR: {}/{}", className, stepName);
            log.error("Error: {}", e.getMessage(), e);
            log.error("========================================");
            lastErrorMessage.set(errorMsg);
            recordMetric(className, stepName, false, 0, tolerance, "ERROR: " + e.getMessage(), null, startTime);
            return false;
        }
    }

    /**
     * Record baselines for the current page.
     */
    private static boolean recordBaselines(WebDriver driver, Path baselineDir, String className,
                                            String stepName, long startTime) throws IOException {
        // Calculate how many screenshots needed
        int screenshotCount = screenshotManager.calculateScreenshotCount(driver);
        log.info("Page requires {} screenshot(s)", screenshotCount);

        // Capture all viewports
        List<BufferedImage> screenshots = screenshotManager.captureAllViewports(driver);

        // Create baseline directory
        Files.createDirectories(baselineDir);

        // Save each screenshot
        for (int i = 0; i < screenshots.size(); i++) {
            Path baselinePath = baselineDir.resolve(stepName + "_" + (i + 1) + ".png");
            screenshotManager.saveImage(screenshots.get(i), baselinePath);
            log.info("RECORDED baseline {}: {}", i + 1, baselinePath);
        }

        log.info("========================================");
        log.info("RECORD COMPLETE: {}/{}", className, stepName);
        log.info("Screenshots saved: {}", screenshots.size());
        log.info("========================================");

        recordMetric(className, stepName, true, 0, 0, "BASELINE_CREATED", null, startTime);
        return true;
    }

    /**
     * Compare current page with baselines.
     */
    private static boolean compareWithBaselines(WebDriver driver, Path baselineDir, String className,
                                                 String stepName, double tolerance, long startTime) throws IOException {
        // Count existing baseline files
        int baselineCount = countBaselineFiles(baselineDir, stepName);

        if (baselineCount == 0) {
            String errorMsg = String.format(
                    "No baselines found for %s/%s at %s. Run with record=true to create baselines.",
                    className, stepName, baselineDir);
            log.error("========================================");
            log.error("BASELINE MISSING");
            log.error(errorMsg);
            log.error("========================================");
            lastErrorMessage.set(errorMsg);
            recordMetric(className, stepName, false, 0, tolerance, "BASELINE_MISSING", null, startTime);
            return false;
        }

        // Calculate how many screenshots current page needs
        int currentCount = screenshotManager.calculateScreenshotCount(driver);

        log.info("Baseline count: {}, Current page needs: {}", baselineCount, currentCount);

        // Check for structure change
        if (baselineCount != currentCount) {
            String errorMsg = String.format(
                    "PAGE STRUCTURE CHANGED: Baseline has %d screenshot(s), but current page needs %d. " +
                    "This indicates the page content height has changed significantly. " +
                    "If this is expected, re-record baselines with record=true.",
                    baselineCount, currentCount);
            log.error("========================================");
            log.error("STRUCTURE CHANGE DETECTED");
            log.error("Baseline screenshots: {}", baselineCount);
            log.error("Current page needs: {}", currentCount);
            log.error("Difference: {} screenshot(s)", Math.abs(baselineCount - currentCount));
            log.error("========================================");
            lastErrorMessage.set(errorMsg);
            recordMetric(className, stepName, false, 0, tolerance,
                    "STRUCTURE_CHANGED: expected=" + baselineCount + ", actual=" + currentCount, null, startTime);
            return false;
        }

        // Capture current screenshots (same count as baselines)
        List<BufferedImage> currentScreenshots = screenshotManager.captureViewports(driver, baselineCount);

        // Compare each pair
        List<ComparisonResult> results = new ArrayList<>();
        boolean allPassed = true;

        for (int i = 0; i < baselineCount; i++) {
            Path baselinePath = baselineDir.resolve(stepName + "_" + (i + 1) + ".png");
            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            BufferedImage current = currentScreenshots.get(i);

            ComparisonResult result = compareSingleScreenshot(baseline, current, tolerance, i + 1);
            results.add(result);

            if (!result.passed) {
                allPassed = false;
                // Save diff and actual for failed comparison
                saveDiffAndActual(result.diffImage, current, className, stepName, i + 1);
            }

            log.info("Screenshot {}: {} (diff: {:.4f}%)",
                    i + 1, result.passed ? "PASS" : "FAIL", result.diffPercentage * 100);
        }

        // Log summary
        logComparisonSummary(className, stepName, results, allPassed);

        // Record metrics
        double maxDiff = results.stream().mapToDouble(r -> r.diffPercentage).max().orElse(0);
        String status = allPassed ? "SUCCESS" : "VISUAL_MISMATCH";
        recordMetric(className, stepName, allPassed, maxDiff, tolerance, status, null, startTime);

        return allPassed;
    }

    /**
     * Compare a single screenshot pair.
     */
    private static ComparisonResult compareSingleScreenshot(BufferedImage baseline, BufferedImage current,
                                                             double tolerance, int index) {
        try {
            HybridVisualComparator.HybridComparisonResult result =
                    getHybridComparator().compare(baseline, current, tolerance, null);

            return new ComparisonResult(
                    index,
                    result.isMatch(),
                    result.getDiffPercentage(),
                    result.getDiffImage(),
                    result.getStrategy().toString(),
                    result.usedAI()
            );
        } catch (Exception e) {
            log.error("Comparison failed for screenshot {}: {}", index, e.getMessage());
            return new ComparisonResult(index, false, 1.0, null, "ERROR", false);
        }
    }

    /**
     * Log comparison summary.
     */
    private static void logComparisonSummary(String className, String stepName,
                                              List<ComparisonResult> results, boolean allPassed) {
        log.info("========================================");
        if (allPassed) {
            log.info("VISUAL VALIDATION PASSED: {}/{}", className, stepName);
        } else {
            log.error("VISUAL VALIDATION FAILED: {}/{}", className, stepName);
        }

        for (ComparisonResult r : results) {
            String status = r.passed ? "✓ PASS" : "✗ FAIL";
            String aiInfo = r.usedAI ? " [AI]" : "";
            log.info("  Screenshot {}: {} - diff: {:.4f}% - strategy: {}{}",
                    r.index, status, r.diffPercentage * 100, r.strategy, aiInfo);
        }

        long passCount = results.stream().filter(r -> r.passed).count();
        log.info("Result: {}/{} screenshots passed", passCount, results.size());
        log.info("========================================");

        if (!allPassed) {
            StringBuilder errorMsg = new StringBuilder("Visual mismatch detected:\n");
            for (ComparisonResult r : results) {
                if (!r.passed) {
                    errorMsg.append(String.format("  - Screenshot %d: %.4f%% difference (tolerance: %.4f%%)\n",
                            r.index, r.diffPercentage * 100, DEFAULT_TOLERANCE * 100));
                }
            }
            lastErrorMessage.set(errorMsg.toString());
        }
    }

    /**
     * Save diff and actual images for failed comparison.
     */
    private static void saveDiffAndActual(BufferedImage diffImage, BufferedImage actualImage,
                                           String className, String stepName, int index) {
        try {
            Path reportDir = getReportVisualDir();
            Files.createDirectories(reportDir);

            String suffix = "_" + index;

            // Save diff
            if (diffImage != null) {
                Path diffPath = reportDir.resolve(className + "_" + stepName + suffix + "_diff.png");
                ImageIO.write(diffImage, "PNG", diffPath.toFile());
                log.info("Diff image saved: {}", diffPath);

                // Store Base64 for embedding (only first diff)
                if (index == 1) {
                    lastDiffBase64.set(imageToBase64(diffImage));
                }
            }

            // Save actual
            Path actualPath = reportDir.resolve(className + "_" + stepName + suffix + "_actual.png");
            ImageIO.write(actualImage, "PNG", actualPath.toFile());
            log.info("Actual image saved: {}", actualPath);

        } catch (IOException e) {
            log.warn("Failed to save diff/actual images: {}", e.getMessage());
        }
    }

    /**
     * Count baseline files for a step.
     * Looks for files named: stepName_1.png, stepName_2.png, etc.
     */
    private static int countBaselineFiles(Path baselineDir, String stepName) {
        if (!Files.exists(baselineDir)) {
            return 0;
        }

        int count = 0;
        while (Files.exists(baselineDir.resolve(stepName + "_" + (count + 1) + ".png"))) {
            count++;
        }

        log.debug("Found {} baseline file(s) for step '{}'", count, stepName);
        return count;
    }

    /**
     * Get baseline directory for browser and class.
     */
    private static Path getBaselineDir(String browserName, String className) {
        Path baselinesRoot = getBaselinesRoot();
        return baselinesRoot.resolve(browserName).resolve(className);
    }

    /**
     * Check if record mode is enabled.
     */
    public static boolean isRecordMode() {
        String recordMode = System.getProperty(RECORD_MODE_PROPERTY);
        return "true".equalsIgnoreCase(recordMode);
    }

    /**
     * Get the last diff image as Base64 (for embedding in reports).
     */
    public static String getLastDiffBase64() {
        return lastDiffBase64.get();
    }

    /**
     * Get the last error message.
     */
    public static String getLastErrorMessage() {
        return lastErrorMessage.get();
    }

    /**
     * Check if AI-based comparison is available.
     */
    public static boolean isAIAvailable() {
        return getHybridComparator().isAIAvailable();
    }

    /**
     * Detect the browser name from the WebDriver instance.
     */
    private static String detectBrowserName(WebDriver driver) {
        if (driver == null) {
            return "unknown";
        }

        // Check by driver class type first (most reliable)
        if (driver instanceof ChromeDriver) {
            return "chrome";
        } else if (driver instanceof FirefoxDriver) {
            return "firefox";
        } else if (driver instanceof EdgeDriver) {
            return "edge";
        } else if (driver instanceof SafariDriver) {
            return "safari";
        } else if (driver instanceof InternetExplorerDriver) {
            return "ie";
        }

        // Fallback: check capabilities for RemoteWebDriver
        if (driver instanceof HasCapabilities) {
            try {
                Capabilities caps = ((HasCapabilities) driver).getCapabilities();
                String browserName = caps.getBrowserName();
                if (browserName != null && !browserName.isEmpty()) {
                    browserName = browserName.toLowerCase().trim();
                    if (browserName.contains("chrome")) return "chrome";
                    if (browserName.contains("firefox")) return "firefox";
                    if (browserName.contains("edge") || browserName.contains("msedge")) return "edge";
                    if (browserName.contains("safari")) return "safari";
                    if (browserName.contains("ie") || browserName.contains("internet explorer")) return "ie";
                    return browserName.replaceAll("[^a-z0-9]", "_");
                }
            } catch (Exception e) {
                log.debug("Could not get browser capabilities: {}", e.getMessage());
            }
        }

        return "unknown";
    }

    /**
     * Get the baselines root directory.
     */
    private static Path getBaselinesRoot() {
        String customRoot = System.getProperty(BASELINES_ROOT_PROPERTY);
        if (customRoot != null && !customRoot.isEmpty()) {
            return Paths.get(customRoot).toAbsolutePath();
        }
        return PROJECT_ROOT.resolve("src").resolve("test").resolve("resources").resolve(BASELINES_DIR_NAME);
    }

    /**
     * Get the report visual directory.
     */
    private static Path getReportVisualDir() {
        return PROJECT_ROOT.resolve(REPORT_VISUAL_DIR_NAME);
    }

    /**
     * Detect the project root.
     */
    private static Path detectProjectRoot() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path dir = currentDir;
        while (dir != null) {
            if (Files.exists(dir.resolve("pom.xml")) || Files.exists(dir.resolve("build.gradle"))) {
                log.debug("Detected project root: {}", dir);
                return dir;
            }
            dir = dir.getParent();
        }
        log.debug("Project root not detected, using user.dir: {}", currentDir);
        return currentDir;
    }

    /**
     * Convert image to Base64 string.
     */
    private static String imageToBase64(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            log.warn("Could not convert image to Base64: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Record visual metric to MetricsCollector.
     */
    private static void recordMetric(String className, String stepName, boolean matched,
                                      double diffPercentage, double tolerance, String status,
                                      String diffImagePath, long startTime) {
        try {
            MetricsCollector collector = MetricsCollector.getInstance();
            if (collector != null) {
                long comparisonTime = System.currentTimeMillis() - startTime;
                String baselineName = className + "/" + stepName;
                collector.recordVisualMetric(
                        className,
                        baselineName,
                        matched,
                        diffPercentage,
                        tolerance,
                        status,
                        diffImagePath,
                        comparisonTime
                );
            }
        } catch (Exception e) {
            log.debug("MetricsCollector not available: {}", e.getMessage());
        }
    }

    /**
     * Result of comparing a single screenshot.
     */
    private static class ComparisonResult {
        final int index;
        final boolean passed;
        final double diffPercentage;
        final BufferedImage diffImage;
        final String strategy;
        final boolean usedAI;

        ComparisonResult(int index, boolean passed, double diffPercentage,
                         BufferedImage diffImage, String strategy, boolean usedAI) {
            this.index = index;
            this.passed = passed;
            this.diffPercentage = diffPercentage;
            this.diffImage = diffImage;
            this.strategy = strategy;
            this.usedAI = usedAI;
        }
    }
}
