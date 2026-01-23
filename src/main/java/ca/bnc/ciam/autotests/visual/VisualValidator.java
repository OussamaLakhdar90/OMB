package ca.bnc.ciam.autotests.visual;

import ca.bnc.ciam.autotests.annotation.SkipVisualCheck;
import ca.bnc.ciam.autotests.annotation.VisualCheckpoint;
import ca.bnc.ciam.autotests.exception.VisualMismatchException;
import ca.bnc.ciam.autotests.visual.model.MismatchBehavior;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.Screenshot;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Visual validation service that orchestrates screenshot capture,
 * baseline management, and image comparison.
 */
@Slf4j
public class VisualValidator {

    private final ScreenshotManager screenshotManager;
    private final BaselineManager baselineManager;
    private final ImageComparator imageComparator;
    private final VisualConfig config;

    /**
     * Create validator with default configuration.
     */
    public VisualValidator() {
        this(VisualConfig.builder().build());
    }

    /**
     * Create validator with custom configuration.
     */
    public VisualValidator(VisualConfig config) {
        this.config = config;
        this.screenshotManager = new ScreenshotManager();
        this.baselineManager = new BaselineManager(config.getBaselinePath());
        this.imageComparator = new ImageComparator(config.getDefaultTolerance());
    }

    /**
     * Create validator with custom managers.
     */
    public VisualValidator(ScreenshotManager screenshotManager,
                           BaselineManager baselineManager,
                           ImageComparator imageComparator,
                           VisualConfig config) {
        this.screenshotManager = screenshotManager;
        this.baselineManager = baselineManager;
        this.imageComparator = imageComparator;
        this.config = config;
    }

    /**
     * Perform visual validation for a test method.
     * Uses annotations to determine validation settings.
     */
    public VisualValidationResult validate(WebDriver driver, Method testMethod, String testClassName) {
        // Check for skip annotation
        SkipVisualCheck skipCheck = testMethod.getAnnotation(SkipVisualCheck.class);
        if (skipCheck != null) {
            log.info("Visual check skipped for {}.{}: {}",
                    testClassName, testMethod.getName(),
                    skipCheck.reason().isEmpty() ? "No reason provided" : skipCheck.reason());
            return VisualValidationResult.skipped(skipCheck.reason());
        }

        // Get visual checkpoint settings
        VisualCheckpoint checkpoint = testMethod.getAnnotation(VisualCheckpoint.class);
        ScreenshotType type = checkpoint != null ? checkpoint.type() : config.getDefaultScreenshotType();
        double tolerance = checkpoint != null ? checkpoint.tolerance() : config.getDefaultTolerance();
        MismatchBehavior behavior = checkpoint != null ? checkpoint.onMismatch() : config.getDefaultMismatchBehavior();
        String suffix = checkpoint != null ? checkpoint.name() : null;
        String[] ignoreRegionsArray = checkpoint != null ? checkpoint.ignoreRegions() : new String[0];

        // Parse ignore regions from array
        List<int[]> ignoreRegions = parseIgnoreRegionsArray(ignoreRegionsArray);

        // Take screenshot
        Screenshot screenshot;
        if (checkpoint != null && !checkpoint.selector().isEmpty()) {
            WebElement element = driver.findElement(By.cssSelector(checkpoint.selector()));
            screenshot = screenshotManager.takeScreenshot(driver, type, element);
        } else {
            screenshot = screenshotManager.takeScreenshot(driver, type);
        }

        // Generate baseline name
        String baselineName = baselineManager.generateBaselineName(testClassName, testMethod.getName(), suffix);

        return validateAgainstBaseline(screenshot, baselineName, tolerance, behavior, ignoreRegions);
    }

    /**
     * Perform visual validation with explicit parameters.
     */
    public VisualValidationResult validate(WebDriver driver, String testClassName, String testMethodName,
                                            ScreenshotType type, double tolerance, MismatchBehavior behavior) {
        Screenshot screenshot = screenshotManager.takeScreenshot(driver, type);
        String baselineName = baselineManager.generateBaselineName(testClassName, testMethodName, null);
        return validateAgainstBaseline(screenshot, baselineName, tolerance, behavior, null);
    }

    /**
     * Validate screenshot against baseline.
     */
    public VisualValidationResult validateAgainstBaseline(Screenshot screenshot, String baselineName,
                                                           double tolerance, MismatchBehavior behavior,
                                                           List<int[]> ignoreRegions) {
        try {
            BufferedImage actualImage = screenshot.getImage();

            // Save actual screenshot
            baselineManager.saveActual(actualImage, baselineName);

            // Check if baseline exists
            if (!baselineManager.baselineExists(baselineName)) {
                if (config.isAutoCreateBaseline()) {
                    baselineManager.saveBaseline(actualImage, baselineName);
                    log.info("Baseline created: {}", baselineName);
                    return VisualValidationResult.baselineCreated(baselineName);
                } else {
                    String message = "Baseline not found and auto-create is disabled: " + baselineName;
                    log.warn(message);
                    return handleMismatch(behavior, message, null, null);
                }
            }

            // Load baseline
            BufferedImage baselineImage = baselineManager.loadBaseline(baselineName);

            // Compare images
            ImageComparator.ComparisonResult comparison = imageComparator.compare(
                    baselineImage, actualImage, tolerance, ignoreRegions);

            if (comparison.isMatch()) {
                log.info("Visual validation PASSED: {}", baselineName);
                return VisualValidationResult.success(baselineName, comparison);
            } else {
                // Save diff image
                if (comparison.getDiffImage() != null) {
                    baselineManager.saveDiff(comparison.getDiffImage(), baselineName);
                }

                String message = String.format("Visual mismatch: %s (diff: %.4f%%, tolerance: %.4f%%)",
                        baselineName, comparison.getDiffPercentage() * 100, tolerance * 100);

                return handleMismatch(behavior, message, comparison, baselineName);
            }

        } catch (IOException e) {
            String message = "Visual validation error: " + e.getMessage();
            log.error(message, e);
            return VisualValidationResult.error(message);
        }
    }

    /**
     * Handle visual mismatch based on behavior setting.
     */
    private VisualValidationResult handleMismatch(MismatchBehavior behavior, String message,
                                                   ImageComparator.ComparisonResult comparison, String baselineName) {
        MismatchBehavior effectiveBehavior = behavior == MismatchBehavior.DEFAULT ?
                config.getDefaultMismatchBehavior() : behavior;

        switch (effectiveBehavior) {
            case FAIL:
                log.error("Visual validation FAILED: {}", message);
                return VisualValidationResult.failure(baselineName, message, comparison);

            case WARN:
                log.warn("Visual validation WARNING: {}", message);
                return VisualValidationResult.warning(baselineName, message, comparison);

            case IGNORE:
                log.info("Visual validation IGNORED: {}", message);
                return VisualValidationResult.ignored(message);

            default:
                return VisualValidationResult.failure(baselineName, message, comparison);
        }
    }

    /**
     * Take screenshot without comparison (for manual inspection or baseline creation).
     */
    public Path captureScreenshot(WebDriver driver, String name, ScreenshotType type) throws IOException {
        Screenshot screenshot = screenshotManager.takeScreenshot(driver, type);
        return baselineManager.saveActual(screenshot, name + ".png");
    }

    /**
     * Take element screenshot.
     */
    public Path captureElementScreenshot(WebDriver driver, By locator, String name) throws IOException {
        WebElement element = driver.findElement(locator);
        Screenshot screenshot = screenshotManager.takeElementScreenshot(driver, element);
        return baselineManager.saveActual(screenshot, name + ".png");
    }

    /**
     * Update baseline with current screenshot.
     */
    public Path updateBaseline(WebDriver driver, String testClassName, String testMethodName,
                               ScreenshotType type) throws IOException {
        Screenshot screenshot = screenshotManager.takeScreenshot(driver, type);
        String baselineName = baselineManager.generateBaselineName(testClassName, testMethodName, null);
        return baselineManager.updateBaseline(screenshot.getImage(), baselineName);
    }

    /**
     * Check if baseline exists.
     */
    public boolean baselineExists(String testClassName, String testMethodName) {
        return baselineManager.baselineExists(testClassName, testMethodName, null);
    }

    /**
     * Parse ignore regions from String array (CSS selectors or coordinate strings).
     * Supports format "x,y,w,h" for each string in the array.
     */
    private List<int[]> parseIgnoreRegionsArray(String[] regionsArray) {
        List<int[]> regions = new ArrayList<>();
        if (regionsArray == null || regionsArray.length == 0) {
            return regions;
        }

        for (String regionStr : regionsArray) {
            if (regionStr != null && !regionStr.isEmpty()) {
                // Parse as coordinate string "x,y,w,h"
                regions.addAll(ImageComparator.parseIgnoreRegions(regionStr));
            }
        }
        return regions;
    }

    /**
     * Get baseline path.
     */
    public Path getBaselinePath(String testClassName, String testMethodName) {
        return baselineManager.getBaselinePath(testClassName, testMethodName, null);
    }

    /**
     * Visual validation configuration.
     */
    @Data
    @Builder
    public static class VisualConfig {
        @Builder.Default
        private String baselinePath = "src/test/resources/baselines";

        @Builder.Default
        private double defaultTolerance = 0.01; // 1%

        @Builder.Default
        private ScreenshotType defaultScreenshotType = ScreenshotType.FULL_PAGE;

        @Builder.Default
        private MismatchBehavior defaultMismatchBehavior = MismatchBehavior.FAIL;

        @Builder.Default
        private boolean autoCreateBaseline = true;
    }

    /**
     * Visual validation result.
     */
    @Data
    @Builder
    public static class VisualValidationResult {
        private Status status;
        private String baselineName;
        private String message;
        private ImageComparator.ComparisonResult comparisonResult;

        public enum Status {
            SUCCESS, FAILURE, WARNING, SKIPPED, IGNORED, BASELINE_CREATED, ERROR
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS || status == Status.BASELINE_CREATED ||
                    status == Status.SKIPPED || status == Status.IGNORED;
        }

        public boolean shouldFail() {
            return status == Status.FAILURE;
        }

        public static VisualValidationResult success(String baselineName, ImageComparator.ComparisonResult comparison) {
            return VisualValidationResult.builder()
                    .status(Status.SUCCESS)
                    .baselineName(baselineName)
                    .message("Visual validation passed")
                    .comparisonResult(comparison)
                    .build();
        }

        public static VisualValidationResult failure(String baselineName, String message,
                                                      ImageComparator.ComparisonResult comparison) {
            return VisualValidationResult.builder()
                    .status(Status.FAILURE)
                    .baselineName(baselineName)
                    .message(message)
                    .comparisonResult(comparison)
                    .build();
        }

        public static VisualValidationResult warning(String baselineName, String message,
                                                      ImageComparator.ComparisonResult comparison) {
            return VisualValidationResult.builder()
                    .status(Status.WARNING)
                    .baselineName(baselineName)
                    .message(message)
                    .comparisonResult(comparison)
                    .build();
        }

        public static VisualValidationResult skipped(String reason) {
            return VisualValidationResult.builder()
                    .status(Status.SKIPPED)
                    .message("Skipped: " + (reason.isEmpty() ? "No reason provided" : reason))
                    .build();
        }

        public static VisualValidationResult ignored(String message) {
            return VisualValidationResult.builder()
                    .status(Status.IGNORED)
                    .message(message)
                    .build();
        }

        public static VisualValidationResult baselineCreated(String baselineName) {
            return VisualValidationResult.builder()
                    .status(Status.BASELINE_CREATED)
                    .baselineName(baselineName)
                    .message("Baseline created: " + baselineName)
                    .build();
        }

        public static VisualValidationResult error(String message) {
            return VisualValidationResult.builder()
                    .status(Status.ERROR)
                    .message(message)
                    .build();
        }
    }
}
