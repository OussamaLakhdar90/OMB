package ca.bnc.ciam.autotests.utils;

import ca.bnc.ciam.autotests.metrics.MetricsCollector;
import ca.bnc.ciam.autotests.visual.HybridVisualComparator;
import ca.bnc.ciam.autotests.visual.ScreenshotManager;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import ru.yandex.qatools.ashot.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Visual capture utility for screenshot recording and comparison.
 *
 * Uses a hybrid comparison approach:
 * 1. Fast pixel-based comparison (OpenCV)
 * 2. AI fallback (DJL + ResNet18) for uncertain cases (gray zone)
 *
 * Usage:
 * <pre>
 * // In debug_config.json: { "record": true }  → Record mode (creates baselines)
 * // In debug_config.json: { "record": false } → Compare mode (validates against baselines)
 *
 * boolean passed = VisualCapture.captureStep(driver, "LoginTest", "t001_login_page");
 * assertThat(passed).as("Visual check failed").isTrue();
 * </pre>
 *
 * Files are saved under: {projectRoot}/src/test/resources/baselines/{ClassName}/{stepName}.png
 * Diff images for reports: {projectRoot}/target/metrics/visual/{ClassName}_{stepName}_diff.png
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

    private VisualCapture() {
        // Utility class - prevent instantiation
    }

    /**
     * Capture a visual step.
     *
     * If record mode is enabled (bnc.record.mode=true):
     *   - Takes screenshot and saves as baseline
     *   - Returns true (always passes in record mode)
     *
     * If record mode is disabled (bnc.record.mode=false or not set):
     *   - Takes screenshot and compares against baseline
     *   - Returns true if images match within tolerance, false otherwise
     *
     * @param driver    the WebDriver instance
     * @param className the test class name (used for folder)
     * @param stepName  the step name (used for file name)
     * @return true if passed (or recording), false if mismatch
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName) {
        return captureStep(driver, className, stepName, ScreenshotType.FULL_PAGE, DEFAULT_TOLERANCE);
    }

    /**
     * Capture a visual step with custom screenshot type.
     *
     * @param driver    the WebDriver instance
     * @param className the test class name (used for folder)
     * @param stepName  the step name (used for file name)
     * @param type      the screenshot type (FULL_PAGE, VIEWPORT)
     * @return true if passed (or recording), false if mismatch
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName, ScreenshotType type) {
        return captureStep(driver, className, stepName, type, DEFAULT_TOLERANCE);
    }

    /**
     * Capture a visual step with custom screenshot type and tolerance.
     *
     * @param driver    the WebDriver instance
     * @param className the test class name (used for folder)
     * @param stepName  the step name (used for file name)
     * @param type      the screenshot type (FULL_PAGE, VIEWPORT)
     * @param tolerance the comparison tolerance (0.0 to 1.0)
     * @return true if passed (or recording), false if mismatch
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName,
                                       ScreenshotType type, double tolerance) {
        boolean isRecordMode = isRecordMode();
        long startTime = System.currentTimeMillis();

        // Get baseline path (absolute)
        Path baselinePath = getBaselinePath(className, stepName);

        log.info("Visual capture: {}/{} - Mode: {} - Baseline: {}",
                className, stepName, isRecordMode ? "RECORD" : "COMPARE", baselinePath);

        // Clear previous diff
        lastDiffBase64.remove();

        try {
            // Take screenshot
            Screenshot screenshot = screenshotManager.takeScreenshot(driver, type);
            BufferedImage currentImage = screenshot.getImage();

            if (isRecordMode) {
                // RECORD MODE: Save as baseline
                boolean success = saveBaseline(currentImage, baselinePath, className, stepName);
                recordMetric(className, stepName, true, 0, tolerance, "BASELINE_CREATED", null, startTime);
                return success;
            } else {
                // COMPARE MODE: Compare with baseline
                return compareWithBaseline(currentImage, baselinePath, className, stepName, tolerance, startTime);
            }

        } catch (Exception e) {
            log.error("Visual capture failed for {}/{}: {}", className, stepName, e.getMessage(), e);
            recordMetric(className, stepName, false, 0, tolerance, "ERROR", null, startTime);
            return false;
        }
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
     * Check if AI-based comparison is available.
     */
    public static boolean isAIAvailable() {
        return getHybridComparator().isAIAvailable();
    }

    /**
     * Get the baseline path for a class and step.
     * Uses absolute path based on project root for consistent resolution.
     */
    private static Path getBaselinePath(String className, String stepName) {
        Path baselinesRoot = getBaselinesRoot();
        return baselinesRoot.resolve(className).resolve(stepName + ".png");
    }

    /**
     * Get the baselines root directory.
     * Priority: 1) bnc.baselines.root system property, 2) {projectRoot}/src/test/resources/baselines
     */
    private static Path getBaselinesRoot() {
        String customRoot = System.getProperty(BASELINES_ROOT_PROPERTY);
        if (customRoot != null && !customRoot.isEmpty()) {
            return Paths.get(customRoot).toAbsolutePath();
        }
        return PROJECT_ROOT.resolve("src/test/resources").resolve(BASELINES_DIR_NAME);
    }

    /**
     * Detect the project root by looking for pom.xml or build.gradle.
     * Falls back to user.dir if not found.
     */
    private static Path detectProjectRoot() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        // Walk up the directory tree to find project root
        Path dir = currentDir;
        while (dir != null) {
            if (Files.exists(dir.resolve("pom.xml")) || Files.exists(dir.resolve("build.gradle"))) {
                log.debug("Detected project root: {}", dir);
                return dir;
            }
            dir = dir.getParent();
        }

        // Fall back to current directory
        log.debug("Project root not detected, using user.dir: {}", currentDir);
        return currentDir;
    }

    /**
     * Save screenshot as baseline.
     */
    private static boolean saveBaseline(BufferedImage image, Path baselinePath,
                                         String className, String stepName) {
        try {
            // Create directory if it doesn't exist
            Path parentDir = baselinePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("Created baseline directory: {}", parentDir);
            }

            // Save image
            ImageIO.write(image, "PNG", baselinePath.toFile());
            log.info("RECORDED baseline: {}/{} -> {}", className, stepName, baselinePath);

            return true;

        } catch (IOException e) {
            log.error("Failed to save baseline {}/{}: {}", className, stepName, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compare screenshot with baseline using hybrid comparison (pixel + AI fallback).
     */
    private static boolean compareWithBaseline(BufferedImage currentImage, Path baselinePath,
                                                String className, String stepName, double tolerance,
                                                long startTime) {
        try {
            // Check if baseline exists
            if (!Files.exists(baselinePath)) {
                log.error("FAIL: Baseline not found for {}/{} at {}", className, stepName, baselinePath);
                log.error("Run with record=true in debug_config.json to create baseline");
                recordMetric(className, stepName, false, 0, tolerance, "BASELINE_MISSING", null, startTime);
                return false;
            }

            // Load baseline
            BufferedImage baselineImage = ImageIO.read(baselinePath.toFile());

            // Compare using hybrid strategy (pixel-based + AI fallback)
            HybridVisualComparator.HybridComparisonResult result =
                    getHybridComparator().compare(baselineImage, currentImage, tolerance, null);

            String diffImagePath = null;

            if (result.isMatch()) {
                String strategyInfo = result.usedAI()
                        ? String.format(" [AI: %.2f%%]", result.getAiResult().getSimilarity() * 100)
                        : "";
                log.info("PASS: Visual match for {}/{} (diff: {}%, strategy: {}){}",
                        className, stepName,
                        String.format("%.4f", result.getDiffPercentage() * 100),
                        result.getStrategy(),
                        strategyInfo);
                recordMetric(className, stepName, true, result.getDiffPercentage(), tolerance,
                        "SUCCESS_" + result.getStrategy(), null, startTime);
                return true;
            } else {
                String strategyInfo = result.usedAI()
                        ? String.format(" [AI: %.2f%%]", result.getAiResult().getSimilarity() * 100)
                        : "";
                log.error("FAIL: Visual mismatch for {}/{} (diff: {}%, tolerance: {}%, strategy: {}){}",
                        className, stepName,
                        String.format("%.4f", result.getDiffPercentage() * 100),
                        String.format("%.4f", tolerance * 100),
                        result.getStrategy(),
                        strategyInfo);

                // Save diff image to report folder
                diffImagePath = saveDiffToReport(result.getDiffImage(), className, stepName);

                // Save actual image for reference
                saveActualToReport(currentImage, className, stepName);

                // Store Base64 for embedding
                if (result.getDiffImage() != null) {
                    lastDiffBase64.set(imageToBase64(result.getDiffImage()));
                }

                recordMetric(className, stepName, false, result.getDiffPercentage(), tolerance,
                        "FAILURE_" + result.getStrategy(), diffImagePath, startTime);
                return false;
            }

        } catch (IOException e) {
            log.error("Failed to compare {}/{}: {}", className, stepName, e.getMessage(), e);
            recordMetric(className, stepName, false, 0, tolerance, "ERROR", null, startTime);
            return false;
        }
    }

    /**
     * Get the report visual directory path (absolute).
     */
    private static Path getReportVisualDir() {
        return PROJECT_ROOT.resolve(REPORT_VISUAL_DIR_NAME);
    }

    /**
     * Save diff image to report folder.
     */
    private static String saveDiffToReport(BufferedImage diffImage, String className, String stepName) {
        if (diffImage == null) return null;

        try {
            Path diffPath = getReportVisualDir().resolve(className + "_" + stepName + "_diff.png");
            Files.createDirectories(diffPath.getParent());
            ImageIO.write(diffImage, "PNG", diffPath.toFile());
            log.info("Saved diff image to report: {}", diffPath);
            return diffPath.toString();
        } catch (IOException e) {
            log.warn("Could not save diff image: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Save actual image to report folder.
     */
    private static void saveActualToReport(BufferedImage actualImage, String className, String stepName) {
        try {
            Path actualPath = getReportVisualDir().resolve(className + "_" + stepName + "_actual.png");
            Files.createDirectories(actualPath.getParent());
            ImageIO.write(actualImage, "PNG", actualPath.toFile());
            log.info("Saved actual image to report: {}", actualPath);
        } catch (IOException e) {
            log.warn("Could not save actual image: {}", e.getMessage());
        }
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
}
