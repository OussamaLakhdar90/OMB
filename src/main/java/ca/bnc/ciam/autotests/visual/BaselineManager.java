package ca.bnc.ciam.autotests.visual;

import lombok.extern.slf4j.Slf4j;
import ru.yandex.qatools.ashot.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages baseline images for visual regression testing.
 * Handles storage, retrieval, and updating of baseline screenshots.
 */
@Slf4j
public class BaselineManager {

    private static final String DEFAULT_BASELINE_DIR = "src/test/resources/baselines";
    private static final String ACTUAL_DIR_SUFFIX = "actual";
    private static final String DIFF_DIR_SUFFIX = "diff";
    private static final String IMAGE_FORMAT = "PNG";
    private static final String IMAGE_EXTENSION = ".png";

    private final Path baselineDirectory;
    private final Path actualDirectory;
    private final Path diffDirectory;

    /**
     * Create baseline manager with default directory.
     */
    public BaselineManager() {
        this(DEFAULT_BASELINE_DIR);
    }

    /**
     * Create baseline manager with custom directory.
     */
    public BaselineManager(String baselinePath) {
        this.baselineDirectory = Paths.get(baselinePath);
        this.actualDirectory = baselineDirectory.resolve(ACTUAL_DIR_SUFFIX);
        this.diffDirectory = baselineDirectory.resolve(DIFF_DIR_SUFFIX);

        initializeDirectories();
    }

    /**
     * Initialize required directories.
     */
    private void initializeDirectories() {
        try {
            Files.createDirectories(baselineDirectory);
            Files.createDirectories(actualDirectory);
            Files.createDirectories(diffDirectory);
            log.info("Baseline directories initialized at: {}", baselineDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create baseline directories", e);
        }
    }

    /**
     * Generate baseline file name from test identifiers.
     */
    public String generateBaselineName(String testClass, String testMethod, String suffix) {
        String name = String.format("%s_%s", testClass, testMethod);
        if (suffix != null && !suffix.isEmpty()) {
            name += "_" + suffix;
        }
        return sanitizeFileName(name) + IMAGE_EXTENSION;
    }

    /**
     * Generate baseline file name from test identifiers.
     */
    public String generateBaselineName(String testClass, String testMethod) {
        return generateBaselineName(testClass, testMethod, null);
    }

    /**
     * Get baseline file path.
     */
    public Path getBaselinePath(String fileName) {
        return baselineDirectory.resolve(fileName);
    }

    /**
     * Get baseline file path for test.
     */
    public Path getBaselinePath(String testClass, String testMethod, String suffix) {
        String fileName = generateBaselineName(testClass, testMethod, suffix);
        return getBaselinePath(fileName);
    }

    /**
     * Get actual screenshot file path.
     */
    public Path getActualPath(String fileName) {
        return actualDirectory.resolve(fileName);
    }

    /**
     * Get diff image file path.
     */
    public Path getDiffPath(String fileName) {
        String diffName = fileName.replace(IMAGE_EXTENSION, "_diff" + IMAGE_EXTENSION);
        return diffDirectory.resolve(diffName);
    }

    /**
     * Check if baseline exists.
     */
    public boolean baselineExists(String fileName) {
        return Files.exists(getBaselinePath(fileName));
    }

    /**
     * Check if baseline exists for test.
     */
    public boolean baselineExists(String testClass, String testMethod, String suffix) {
        String fileName = generateBaselineName(testClass, testMethod, suffix);
        return baselineExists(fileName);
    }

    /**
     * Load baseline image.
     */
    public BufferedImage loadBaseline(String fileName) throws IOException {
        Path path = getBaselinePath(fileName);
        if (!Files.exists(path)) {
            throw new IOException("Baseline not found: " + path);
        }
        log.debug("Loading baseline from: {}", path);
        return ImageIO.read(path.toFile());
    }

    /**
     * Load baseline image for test.
     */
    public BufferedImage loadBaseline(String testClass, String testMethod, String suffix) throws IOException {
        String fileName = generateBaselineName(testClass, testMethod, suffix);
        return loadBaseline(fileName);
    }

    /**
     * Save baseline image.
     */
    public Path saveBaseline(BufferedImage image, String fileName) throws IOException {
        Path path = getBaselinePath(fileName);
        Files.createDirectories(path.getParent());
        ImageIO.write(image, IMAGE_FORMAT, path.toFile());
        log.info("Baseline saved to: {}", path);
        return path;
    }

    /**
     * Save baseline from screenshot.
     */
    public Path saveBaseline(Screenshot screenshot, String fileName) throws IOException {
        return saveBaseline(screenshot.getImage(), fileName);
    }

    /**
     * Save baseline for test.
     */
    public Path saveBaseline(BufferedImage image, String testClass, String testMethod, String suffix) throws IOException {
        String fileName = generateBaselineName(testClass, testMethod, suffix);
        return saveBaseline(image, fileName);
    }

    /**
     * Save actual screenshot.
     */
    public Path saveActual(BufferedImage image, String fileName) throws IOException {
        Path path = getActualPath(fileName);
        Files.createDirectories(path.getParent());
        ImageIO.write(image, IMAGE_FORMAT, path.toFile());
        log.debug("Actual screenshot saved to: {}", path);
        return path;
    }

    /**
     * Save actual from screenshot.
     */
    public Path saveActual(Screenshot screenshot, String fileName) throws IOException {
        return saveActual(screenshot.getImage(), fileName);
    }

    /**
     * Save diff image.
     */
    public Path saveDiff(BufferedImage image, String fileName) throws IOException {
        Path path = getDiffPath(fileName);
        Files.createDirectories(path.getParent());
        ImageIO.write(image, IMAGE_FORMAT, path.toFile());
        log.info("Diff image saved to: {}", path);
        return path;
    }

    /**
     * Update baseline with new image (backup old baseline).
     */
    public Path updateBaseline(BufferedImage newImage, String fileName) throws IOException {
        Path baselinePath = getBaselinePath(fileName);

        // Backup existing baseline
        if (Files.exists(baselinePath)) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String backupName = fileName.replace(IMAGE_EXTENSION, "_backup_" + timestamp + IMAGE_EXTENSION);
            Path backupPath = baselineDirectory.resolve("backup").resolve(backupName);
            Files.createDirectories(backupPath.getParent());
            Files.copy(baselinePath, backupPath);
            log.info("Baseline backed up to: {}", backupPath);
        }

        // Save new baseline
        return saveBaseline(newImage, fileName);
    }

    /**
     * Delete baseline.
     */
    public boolean deleteBaseline(String fileName) throws IOException {
        Path path = getBaselinePath(fileName);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Baseline deleted: {}", path);
            return true;
        }
        return false;
    }

    /**
     * Clean up actual and diff images older than specified days.
     */
    public int cleanupOldImages(int daysOld) throws IOException {
        int deleted = 0;
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);

        deleted += cleanupDirectory(actualDirectory, cutoffTime);
        deleted += cleanupDirectory(diffDirectory, cutoffTime);

        log.info("Cleaned up {} old images", deleted);
        return deleted;
    }

    /**
     * Clean up old files in a directory.
     */
    private int cleanupDirectory(Path directory, long cutoffTime) throws IOException {
        int deleted = 0;
        if (Files.exists(directory)) {
            for (File file : directory.toFile().listFiles()) {
                if (file.isFile() && file.lastModified() < cutoffTime) {
                    if (file.delete()) {
                        deleted++;
                    }
                }
            }
        }
        return deleted;
    }

    /**
     * Sanitize file name.
     */
    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Get baseline directory path.
     */
    public Path getBaselineDirectory() {
        return baselineDirectory;
    }

    /**
     * Get actual directory path.
     */
    public Path getActualDirectory() {
        return actualDirectory;
    }

    /**
     * Get diff directory path.
     */
    public Path getDiffDirectory() {
        return diffDirectory;
    }
}
