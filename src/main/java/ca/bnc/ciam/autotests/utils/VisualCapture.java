package ca.bnc.ciam.autotests.utils;

import ca.bnc.ciam.autotests.metrics.MetricsCollector;
import ca.bnc.ciam.autotests.visual.HybridVisualComparator;
import ca.bnc.ciam.autotests.visual.ScreenshotManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ca.bnc.ciam.autotests.web.elements.IElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.safari.SafariDriver;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

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
 * - bnc.web.gui.lang: language code for baselines (e.g., "en", "fr")
 * - lang: fallback language property
 *
 * Baseline structure:
 * - baselines/{browser}/{language}/{className}/{stepName}_1.png
 * - Example: baselines/chrome/en/LoginTest/login_page_1.png
 */
@Slf4j
public final class VisualCapture {

    private static final String BASELINES_DIR_NAME = "baselines";
    private static final String REPORT_VISUAL_DIR_NAME = "target/metrics/visual";
    private static final String RECORD_MODE_PROPERTY = "bnc.record.mode";
    private static final String BASELINES_ROOT_PROPERTY = "bnc.baselines.root";
    private static final String AI_ENABLED_PROPERTY = "bnc.visual.ai.enabled";
    private static final String LANGUAGE_PROPERTY = "bnc.web.gui.lang";
    private static final String LANGUAGE_PROPERTY_FALLBACK = "lang";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final double DEFAULT_TOLERANCE = 0.003; // 0.3% - very strict tolerance

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
        return captureStep(driver, className, stepName, tolerance, (List<int[]>) null);
    }

    /**
     * Capture a visual step with elements to ignore.
     * Dynamic content (timestamps, counters, etc.) can be excluded from comparison.
     *
     * <p>Example usage:
     * <pre>
     * WebElement timestamp = driver.findElement(By.id("timestamp"));
     * WebElement userAvatar = driver.findElement(By.className("avatar"));
     * boolean passed = VisualCapture.captureStepIgnoring(driver, "LoginTest", "dashboard", timestamp, userAvatar);
     * </pre>
     *
     * @param driver          the WebDriver instance
     * @param className       the test class name (used for folder)
     * @param stepName        the step name (used for file prefix)
     * @param elementsToIgnore elements to exclude from visual comparison (timestamps, dynamic content, etc.)
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStepIgnoring(WebDriver driver, String className, String stepName,
                                               WebElement... elementsToIgnore) {
        return captureStepIgnoring(driver, className, stepName, DEFAULT_TOLERANCE, elementsToIgnore);
    }

    /**
     * Capture a visual step with custom tolerance and elements to ignore.
     *
     * @param driver          the WebDriver instance
     * @param className       the test class name (used for folder)
     * @param stepName        the step name (used for file prefix)
     * @param tolerance       the comparison tolerance (0.0 to 1.0)
     * @param elementsToIgnore elements to exclude from visual comparison
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStepIgnoring(WebDriver driver, String className, String stepName,
                                               double tolerance, WebElement... elementsToIgnore) {
        List<int[]> ignoreRegions = convertElementsToRegions(elementsToIgnore);
        return captureStep(driver, className, stepName, tolerance, ignoreRegions);
    }

    /**
     * Capture a visual step with IElement wrappers to ignore.
     * Accepts framework element wrappers (Element, TextField, Button, CheckBox, Image).
     *
     * <p>Example usage:
     * <pre>
     * IElement timestamp = pageObject.getElement("timestamp");
     * IElement counter = pageObject.getElement("visitor-counter");
     * boolean passed = VisualCapture.captureStepIgnoring(driver, "LoginTest", "dashboard", timestamp, counter);
     * </pre>
     *
     * @param driver          the WebDriver instance
     * @param className       the test class name (used for folder)
     * @param stepName        the step name (used for file prefix)
     * @param elementsToIgnore IElement wrappers to exclude from visual comparison
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStepIgnoring(WebDriver driver, String className, String stepName,
                                               IElement... elementsToIgnore) {
        return captureStepIgnoring(driver, className, stepName, DEFAULT_TOLERANCE, elementsToIgnore);
    }

    /**
     * Capture a visual step with custom tolerance and IElement wrappers to ignore.
     *
     * @param driver          the WebDriver instance
     * @param className       the test class name (used for folder)
     * @param stepName        the step name (used for file prefix)
     * @param tolerance       the comparison tolerance (0.0 to 1.0)
     * @param elementsToIgnore IElement wrappers to exclude from visual comparison
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStepIgnoring(WebDriver driver, String className, String stepName,
                                               double tolerance, IElement... elementsToIgnore) {
        List<int[]> ignoreRegions = convertIElementsToRegions(elementsToIgnore);
        return captureStep(driver, className, stepName, tolerance, ignoreRegions);
    }

    /**
     * Capture a visual step with custom tolerance and ignore regions (coordinates).
     * Each region is an int array: [x, y, width, height]
     *
     * <p>Example usage:
     * <pre>
     * List&lt;int[]&gt; ignoreRegions = Arrays.asList(
     *     new int[]{100, 50, 200, 30},  // Ignore area at (100,50) with size 200x30
     *     new int[]{0, 0, 150, 100}     // Ignore top-left corner
     * );
     * boolean passed = VisualCapture.captureStep(driver, "LoginTest", "dashboard", 0.01, ignoreRegions);
     * </pre>
     *
     * @param driver        the WebDriver instance
     * @param className     the test class name (used for folder)
     * @param stepName      the step name (used for file prefix)
     * @param tolerance     the comparison tolerance (0.0 to 1.0)
     * @param ignoreRegions list of regions to ignore, each as [x, y, width, height]
     * @return true if passed (or recording), false if mismatch or error
     */
    public static boolean captureStep(WebDriver driver, String className, String stepName,
                                       double tolerance, List<int[]> ignoreRegions) {
        // Validate driver is not null
        if (driver == null) {
            log.error("Cannot capture visual step: WebDriver is null");
            lastErrorMessage.set("WebDriver is null - cannot capture visual step");
            return false;
        }

        boolean isRecordMode = isRecordMode();
        long startTime = System.currentTimeMillis();

        // Clear previous state
        lastDiffBase64.remove();
        lastErrorMessage.remove();

        // Get browser and language-specific baseline directory
        String browserName = detectBrowserName(driver);
        String language = getLanguage();
        Path baselineDir = getBaselineDir(browserName, className);

        boolean isLocalExecution = isLocalExecution();

        log.info("========================================");
        log.info("Visual Capture: {}/{}", className, stepName);
        log.info("Browser: {}, Language: {}", browserName, language);
        log.info("Mode: {}", isRecordMode ? "RECORD" : "COMPARE");
        log.info("Execution: {}", isLocalExecution ? "LOCAL (laptop/desktop)" : "PIPELINE (SauceLabs)");
        log.info("Baseline directory: {}", baselineDir);
        if (!isRecordMode && isLocalExecution) {
            log.info("NOTE: Local execution may use scaled comparison if resolution differs from baseline");
        }
        if (ignoreRegions != null && !ignoreRegions.isEmpty()) {
            log.info("Ignore regions: {} area(s) will be excluded from comparison", ignoreRegions.size());
            for (int i = 0; i < ignoreRegions.size(); i++) {
                int[] r = ignoreRegions.get(i);
                log.info("  Region {}: x={}, y={}, w={}, h={}", i + 1, r[0], r[1], r[2], r[3]);
            }
        }
        log.info("========================================");

        try {
            // Set standard resolution for consistency
            Dimension originalSize = screenshotManager.ensureStandardResolution(driver);
            log.info("Window size set to {}x{}", ScreenshotManager.DEFAULT_WIDTH, ScreenshotManager.DEFAULT_HEIGHT);

            boolean result;
            if (isRecordMode) {
                result = recordBaselines(driver, baselineDir, className, stepName, startTime);
            } else {
                result = compareWithBaselines(driver, baselineDir, className, stepName, tolerance, ignoreRegions, startTime);
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
            recordMetric(className, stepName, false, 0, tolerance, "ERROR: " + e.getMessage(), null, null, startTime);
            return false;
        }
    }

    /**
     * Record baselines for the current page.
     * In record mode, existing baselines are OVERWRITTEN (no comparison is performed).
     */
    private static boolean recordBaselines(WebDriver driver, Path baselineDir, String className,
                                            String stepName, long startTime) throws IOException {
        // Check if baselines already exist
        int existingCount = countBaselineFiles(baselineDir, stepName);
        if (existingCount > 0) {
            log.info("RECORD MODE: Existing baselines found ({} files) - will be OVERWRITTEN", existingCount);
        }

        // Calculate how many screenshots needed
        int screenshotCount = screenshotManager.calculateScreenshotCount(driver);
        log.info("Page requires {} screenshot(s)", screenshotCount);

        // Capture all viewports
        List<BufferedImage> screenshots = screenshotManager.captureAllViewports(driver);

        // Create baseline directory
        Files.createDirectories(baselineDir);

        // Save each screenshot (overwrites existing files)
        int newCount = 0;
        int overwrittenCount = 0;
        for (int i = 0; i < screenshots.size(); i++) {
            Path baselinePath = baselineDir.resolve(stepName + "_" + (i + 1) + ".png");
            boolean existed = Files.exists(baselinePath);
            screenshotManager.saveImage(screenshots.get(i), baselinePath);

            if (existed) {
                log.info("OVERWRITTEN baseline {}: {}", i + 1, baselinePath);
                overwrittenCount++;
            } else {
                log.info("CREATED baseline {}: {}", i + 1, baselinePath);
                newCount++;
            }
        }

        // Clean up old baselines if current page has fewer screenshots
        int removedCount = 0;
        if (existingCount > screenshots.size()) {
            for (int i = screenshots.size(); i < existingCount; i++) {
                Path oldBaseline = baselineDir.resolve(stepName + "_" + (i + 1) + ".png");
                if (Files.deleteIfExists(oldBaseline)) {
                    log.info("REMOVED stale baseline: {}", oldBaseline);
                    removedCount++;
                }
            }
        }

        log.info("========================================");
        log.info("RECORD COMPLETE: {}/{}", className, stepName);
        log.info("New: {}, Overwritten: {}, Removed: {}", newCount, overwrittenCount, removedCount);
        log.info("========================================");

        recordMetric(className, stepName, true, 0, 0, "BASELINE_CREATED", null, null, startTime);
        return true;
    }

    /**
     * Compare current page with baselines.
     */
    private static boolean compareWithBaselines(WebDriver driver, Path baselineDir, String className,
                                                 String stepName, double tolerance, List<int[]> ignoreRegions,
                                                 long startTime) throws IOException {
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
            recordMetric(className, stepName, false, 0, tolerance, "BASELINE_MISSING", null, null, startTime);
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
                    "STRUCTURE_CHANGED: expected=" + baselineCount + ", actual=" + currentCount, null, null, startTime);
            return false;
        }

        // Capture current screenshots (same count as baselines)
        List<BufferedImage> currentScreenshots = screenshotManager.captureViewports(driver, baselineCount);

        // Compare each pair
        List<ComparisonResult> results = new ArrayList<>();
        boolean allPassed = true;
        String firstDiffImagePath = null;
        String firstActualImagePath = null;

        for (int i = 0; i < baselineCount; i++) {
            Path baselinePath = baselineDir.resolve(stepName + "_" + (i + 1) + ".png");
            BufferedImage baseline = ImageIO.read(baselinePath.toFile());
            BufferedImage current = currentScreenshots.get(i);

            ComparisonResult result = compareSingleScreenshot(baseline, current, tolerance, ignoreRegions, i + 1);
            results.add(result);

            if (!result.passed) {
                allPassed = false;
                // Save diff and actual for failed comparison, capture paths for report
                String[] paths = saveDiffAndActual(result.diffImage, current, className, stepName, i + 1);
                if (paths != null && firstDiffImagePath == null) {
                    firstDiffImagePath = paths[0];
                    firstActualImagePath = paths[1];
                }
            }

            log.info("Screenshot {}: {} (diff: {:.4f}%)",
                    i + 1, result.passed ? "PASS" : "FAIL", result.diffPercentage * 100);
        }

        // Log summary
        logComparisonSummary(className, stepName, results, allPassed);

        // Record metrics with diff and actual image paths for report
        double maxDiff = results.stream().mapToDouble(r -> r.diffPercentage).max().orElse(0);
        String status = allPassed ? "SUCCESS" : "VISUAL_MISMATCH";
        recordMetric(className, stepName, allPassed, maxDiff, tolerance, status, firstDiffImagePath, firstActualImagePath, startTime);

        return allPassed;
    }

    /**
     * Compare a single screenshot pair.
     */
    private static ComparisonResult compareSingleScreenshot(BufferedImage baseline, BufferedImage current,
                                                             double tolerance, List<int[]> ignoreRegions, int index) {
        try {
            HybridVisualComparator.HybridComparisonResult result =
                    getHybridComparator().compare(baseline, current, tolerance, ignoreRegions);

            return new ComparisonResult(
                    index,
                    result.isMatch(),
                    result.getDiffPercentage(),
                    result.getDiffImage(),
                    result.getStrategy().toString(),
                    result.usedAI(),
                    result.isWasScaled(),
                    result.getScaleFactor()
            );
        } catch (Exception e) {
            log.error("Comparison failed for screenshot {}: {}", index, e.getMessage());
            return new ComparisonResult(index, false, 1.0, null, "ERROR", false, false, 1.0);
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

        // Check if any result was scaled
        boolean anyScaled = results.stream().anyMatch(r -> r.wasScaled);
        if (anyScaled) {
            log.info("NOTE: Comparison performed with SCALED images (local resolution differs from baseline)");
        }

        for (ComparisonResult r : results) {
            String status = r.passed ? "✓ PASS" : "✗ FAIL";
            String aiInfo = r.usedAI ? " [AI]" : "";
            String scaledInfo = r.wasScaled ? String.format(" [SCALED %.2fx]", r.scaleFactor) : "";
            log.info("  Screenshot {}: {} - diff: {:.4f}% - strategy: {}{}{}",
                    r.index, status, r.diffPercentage * 100, r.strategy, aiInfo, scaledInfo);
        }

        long passCount = results.stream().filter(r -> r.passed).count();
        log.info("Result: {}/{} screenshots passed", passCount, results.size());
        log.info("========================================");

        if (!allPassed) {
            StringBuilder errorMsg = new StringBuilder("Visual mismatch detected:\n");
            for (ComparisonResult r : results) {
                if (!r.passed) {
                    String scaledNote = r.wasScaled ? " (scaled comparison)" : "";
                    errorMsg.append(String.format("  - Screenshot %d: %.4f%% difference (tolerance: %.4f%%)%s\n",
                            r.index, r.diffPercentage * 100, DEFAULT_TOLERANCE * 100, scaledNote));
                }
            }
            lastErrorMessage.set(errorMsg.toString());
        }
    }

    /**
     * Save diff and actual images for failed comparison.
     * Files include language in filename for multi-language support.
     *
     * @return String array [diffPath, actualPath] with relative paths, or null if saving failed
     */
    private static String[] saveDiffAndActual(BufferedImage diffImage, BufferedImage actualImage,
                                               String className, String stepName, int index) {
        try {
            Path reportDir = getReportVisualDir();
            Files.createDirectories(reportDir);

            String language = getLanguage();
            String suffix = "_" + index;
            // Include language in filename: ClassName_lang_stepName_1_diff.png
            String filePrefix = className + "_" + language + "_" + stepName + suffix;

            String diffRelativePath = null;
            String actualRelativePath = null;

            // Save diff
            if (diffImage != null) {
                Path diffPath = reportDir.resolve(filePrefix + "_diff.png");
                ImageIO.write(diffImage, "PNG", diffPath.toFile());
                log.info("Diff image saved: {}", diffPath);

                // Store relative path for report embedding
                diffRelativePath = REPORT_VISUAL_DIR_NAME + "/" + filePrefix + "_diff.png";

                // Store Base64 for embedding (only first diff)
                if (index == 1) {
                    lastDiffBase64.set(imageToBase64(diffImage));
                }
            }

            // Save actual
            Path actualPath = reportDir.resolve(filePrefix + "_actual.png");
            ImageIO.write(actualImage, "PNG", actualPath.toFile());
            log.info("Actual image saved: {}", actualPath);
            actualRelativePath = REPORT_VISUAL_DIR_NAME + "/" + filePrefix + "_actual.png";

            return new String[]{diffRelativePath, actualRelativePath};

        } catch (IOException e) {
            log.warn("Failed to save diff/actual images: {}", e.getMessage());
            return null;
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
     * Get baseline directory for browser, language, and class.
     * Structure: baselines/{browser}/{language}/{className}
     */
    private static Path getBaselineDir(String browserName, String className) {
        Path baselinesRoot = getBaselinesRoot();
        String language = getLanguage();
        return baselinesRoot.resolve(browserName).resolve(language).resolve(className);
    }

    /**
     * Get the current language for baselines.
     * Priority: bnc.web.gui.lang > lang > "en" (default)
     *
     * @return the language code (e.g., "en", "fr")
     */
    public static String getLanguage() {
        String lang = System.getProperty(LANGUAGE_PROPERTY);
        if (lang == null || lang.isEmpty()) {
            lang = System.getProperty(LANGUAGE_PROPERTY_FALLBACK);
        }
        if (lang == null || lang.isEmpty()) {
            lang = DEFAULT_LANGUAGE;
        }
        return lang.toLowerCase().trim();
    }

    /**
     * Check if record mode is enabled.
     * Checks system property first, then falls back to debug_config.json.
     */
    public static boolean isRecordMode() {
        String recordMode = System.getProperty(RECORD_MODE_PROPERTY);

        // If system property not set, try to load from debug_config.json
        if (recordMode == null) {
            recordMode = loadRecordModeFromConfig();
            if (recordMode != null) {
                // Cache the value in system property for subsequent calls
                System.setProperty(RECORD_MODE_PROPERTY, recordMode);
                log.info("Loaded record mode from debug_config.json: {}", recordMode);
            }
        }

        return "true".equalsIgnoreCase(recordMode);
    }

    /**
     * Load record mode from debug_config.json file.
     * @return "true" or "false" string, or null if not found
     */
    private static String loadRecordModeFromConfig() {
        String configPath = "src/test/resources/debug_config.json";
        File configFile = new File(configPath);

        if (!configFile.exists()) {
            log.debug("Debug config file not found at: {}", configPath);
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> config = mapper.readValue(configFile, new TypeReference<>() {});

            Object recordObj = config.get("record");
            if (recordObj != null) {
                return String.valueOf(recordObj);
            }
        } catch (IOException e) {
            log.debug("Could not read debug_config.json: {}", e.getMessage());
        }

        return null;
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
     * Clear the thread-local state (for testing purposes).
     * This clears lastDiffBase64 and lastErrorMessage.
     */
    public static void clearState() {
        lastDiffBase64.remove();
        lastErrorMessage.remove();
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
     * Check if we're running locally (not in pipeline/SauceLabs).
     * The bnc.test.hub.use property is the primary indicator:
     * - If "true" → Pipeline mode (SauceLabs)
     * - If "false" → Local mode (even if URL is configured)
     * - If not set → Check other indicators
     *
     * @return true if running locally, false if running in pipeline
     */
    public static boolean isLocalExecution() {
        // Primary indicator: explicit hub use flag
        String hubUse = System.getProperty("bnc.test.hub.use");

        // If explicitly set, use that value
        if (hubUse != null) {
            boolean isPipeline = "true".equalsIgnoreCase(hubUse);
            log.debug("isLocalExecution: bnc.test.hub.use={} → {}", hubUse, !isPipeline ? "LOCAL" : "PIPELINE");
            return !isPipeline;
        }

        // Fallback: check other indicators only if hub.use is not set
        String sauceUsername = System.getenv("SAUCE_USERNAME");
        boolean hasSauceCredentials = sauceUsername != null && !sauceUsername.isEmpty();

        if (hasSauceCredentials) {
            log.debug("isLocalExecution: SAUCE_USERNAME env var detected → PIPELINE");
            return false;
        }

        // Default to local if no indicators found
        log.debug("isLocalExecution: No pipeline indicators → LOCAL");
        return true;
    }

    /**
     * Record visual metric to MetricsCollector.
     */
    private static void recordMetric(String className, String stepName, boolean matched,
                                      double diffPercentage, double tolerance, String status,
                                      String diffImagePath, String actualImagePath, long startTime) {
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
                        actualImagePath,
                        comparisonTime
                );
            }
        } catch (Exception e) {
            log.debug("MetricsCollector not available: {}", e.getMessage());
        }
    }

    /**
     * Convert WebElements to ignore regions (bounding boxes).
     * Each element's location and size are extracted to create a region [x, y, width, height].
     *
     * @param elements WebElements to convert (null elements are skipped)
     * @return List of int arrays representing regions, or null if no valid elements
     */
    private static List<int[]> convertElementsToRegions(WebElement... elements) {
        if (elements == null || elements.length == 0) {
            return null;
        }

        List<int[]> regions = new ArrayList<>();
        for (WebElement element : elements) {
            if (element == null) {
                continue;
            }
            try {
                org.openqa.selenium.Point location = element.getLocation();
                Dimension size = element.getSize();

                int[] region = new int[]{
                        location.getX(),
                        location.getY(),
                        size.getWidth(),
                        size.getHeight()
                };
                regions.add(region);
                log.debug("Element ignore region: x={}, y={}, w={}, h={}",
                        region[0], region[1], region[2], region[3]);
            } catch (Exception e) {
                log.warn("Could not get bounds for element: {}", e.getMessage());
            }
        }

        return regions.isEmpty() ? null : regions;
    }

    /**
     * Convert IElement wrappers to ignore regions (bounding boxes).
     * Extracts the underlying WebElement from each wrapper and gets its bounds.
     *
     * @param elements IElement wrappers to convert (null elements or null base elements are skipped)
     * @return List of int arrays representing regions, or null if no valid elements
     */
    private static List<int[]> convertIElementsToRegions(IElement... elements) {
        if (elements == null || elements.length == 0) {
            return null;
        }

        List<int[]> regions = new ArrayList<>();
        for (IElement element : elements) {
            if (element == null || element.isNull()) {
                continue;
            }
            try {
                WebElement baseElement = element.getBaseElement();
                org.openqa.selenium.Point location = baseElement.getLocation();
                Dimension size = baseElement.getSize();

                int[] region = new int[]{
                        location.getX(),
                        location.getY(),
                        size.getWidth(),
                        size.getHeight()
                };
                regions.add(region);
                log.debug("IElement ignore region: x={}, y={}, w={}, h={}",
                        region[0], region[1], region[2], region[3]);
            } catch (Exception e) {
                log.warn("Could not get bounds for IElement: {}", e.getMessage());
            }
        }

        return regions.isEmpty() ? null : regions;
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
        final boolean wasScaled;
        final double scaleFactor;

        ComparisonResult(int index, boolean passed, double diffPercentage,
                         BufferedImage diffImage, String strategy, boolean usedAI) {
            this(index, passed, diffPercentage, diffImage, strategy, usedAI, false, 1.0);
        }

        ComparisonResult(int index, boolean passed, double diffPercentage,
                         BufferedImage diffImage, String strategy, boolean usedAI,
                         boolean wasScaled, double scaleFactor) {
            this.index = index;
            this.passed = passed;
            this.diffPercentage = diffPercentage;
            this.diffImage = diffImage;
            this.strategy = strategy;
            this.usedAI = usedAI;
            this.wasScaled = wasScaled;
            this.scaleFactor = scaleFactor;
        }
    }
}
