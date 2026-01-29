package ca.bnc.ciam.autotests.visual;

import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages screenshot capture using native Selenium.
 * Works across ALL browsers without CDP/DevTools dependency.
 *
 * Features:
 * - Fixed resolution: 1920x1080 for consistency
 * - Dynamic scrolling: captures multiple screenshots if page needs scroll
 * - Native Selenium: works on Chrome, Firefox, Edge, Safari, IE
 *
 * Usage:
 * <pre>
 * ScreenshotManager manager = new ScreenshotManager();
 *
 * // Single viewport screenshot
 * Screenshot screenshot = manager.takeViewportScreenshot(driver);
 *
 * // All screenshots needed to capture full page
 * List&lt;BufferedImage&gt; screenshots = manager.captureAllViewports(driver);
 * </pre>
 */
@Slf4j
public class ScreenshotManager {

    public static final int DEFAULT_WIDTH = 1920;
    public static final int DEFAULT_HEIGHT = 1080;
    private static final int SCROLL_WAIT_MS = 200;

    private final AShot elementAshot;

    public ScreenshotManager() {
        this.elementAshot = new AShot()
                .coordsProvider(new WebDriverCoordsProvider());
    }

    /**
     * Take screenshot based on type.
     */
    public Screenshot takeScreenshot(WebDriver driver, ScreenshotType type) {
        return switch (type) {
            case FULL_PAGE -> takeFullPageScreenshot(driver);
            case VIEWPORT -> takeViewportScreenshot(driver);
            case ELEMENT -> throw new IllegalArgumentException("Element type requires element parameter");
            case BOTH -> takeFullPageScreenshot(driver);
        };
    }

    /**
     * Take screenshot based on type with element.
     */
    public Screenshot takeScreenshot(WebDriver driver, ScreenshotType type, WebElement element) {
        return switch (type) {
            case ELEMENT -> takeElementScreenshot(driver, element);
            case FULL_PAGE -> takeFullPageScreenshot(driver);
            case VIEWPORT -> takeViewportScreenshot(driver);
            case BOTH -> takeFullPageScreenshot(driver);
        };
    }

    /**
     * Ensure window is set to standard resolution (1920x1080).
     * This provides consistency across different machines and browsers.
     *
     * @param driver the WebDriver instance
     * @return the original window size (for restoration if needed)
     */
    public Dimension ensureStandardResolution(WebDriver driver) {
        Dimension originalSize = driver.manage().window().getSize();

        if (originalSize.getWidth() != DEFAULT_WIDTH || originalSize.getHeight() != DEFAULT_HEIGHT) {
            log.info("Setting window size to {}x{} (was {}x{})",
                    DEFAULT_WIDTH, DEFAULT_HEIGHT, originalSize.getWidth(), originalSize.getHeight());

            try {
                // First, try to restore window from maximized state
                // This prevents "failed to change window state to 'normal'" error
                try {
                    // Use JavaScript to check if window appears maximized
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    Boolean isMaximized = (Boolean) js.executeScript(
                        "return window.outerWidth >= screen.availWidth && window.outerHeight >= screen.availHeight;");

                    if (Boolean.TRUE.equals(isMaximized)) {
                        log.debug("Window appears maximized, restoring to normal state first");
                        // Set a smaller size first to force window out of maximized state
                        driver.manage().window().setSize(new Dimension(DEFAULT_WIDTH - 1, DEFAULT_HEIGHT - 1));
                        Thread.sleep(50);
                    }
                } catch (Exception e) {
                    log.debug("Could not check/restore maximized state via JS: {}", e.getMessage());
                }

                // Now set the target size
                driver.manage().window().setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

            } catch (Exception e) {
                // If setSize fails (e.g., window is maximized), try alternative approach
                log.warn("Failed to resize window directly: {}. Trying alternative approach.", e.getMessage());
                try {
                    // Try to set a different size first, then the target size
                    driver.manage().window().setSize(new Dimension(800, 600));
                    Thread.sleep(100);
                    driver.manage().window().setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
                } catch (Exception e2) {
                    log.warn("Alternative resize also failed: {}. Proceeding with current window size.", e2.getMessage());
                    // Continue anyway - visual comparison might still work with different size
                }
            }

            // Wait for resize to take effect
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return originalSize;
    }

    /**
     * Restore window to a specific size.
     *
     * @param driver the WebDriver instance
     * @param size the size to restore to
     */
    public void restoreWindowSize(WebDriver driver, Dimension size) {
        driver.manage().window().setSize(size);
    }

    /**
     * Calculate how many viewport screenshots are needed to capture the full page.
     *
     * @param driver the WebDriver instance
     * @return number of screenshots needed (minimum 1)
     */
    public int calculateScreenshotCount(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Get page and viewport dimensions
        long scrollHeight = ((Number) js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);")).longValue();
        long viewportHeight = ((Number) js.executeScript("return window.innerHeight;")).longValue();

        if (viewportHeight <= 0) {
            log.warn("Invalid viewport height: {}, defaulting to 1 screenshot", viewportHeight);
            return 1;
        }

        int count = (int) Math.ceil((double) scrollHeight / viewportHeight);

        log.debug("Page scrollHeight={}, viewportHeight={}, screenshots needed={}",
                scrollHeight, viewportHeight, count);

        return Math.max(1, count);
    }

    /**
     * Check if the page has vertical scroll.
     *
     * @param driver the WebDriver instance
     * @return true if page has scroll, false otherwise
     */
    public boolean hasVerticalScroll(WebDriver driver) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        long scrollHeight = ((Number) js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);")).longValue();
        long viewportHeight = ((Number) js.executeScript("return window.innerHeight;")).longValue();

        boolean hasScroll = scrollHeight > viewportHeight;
        log.debug("Has vertical scroll: {} (scrollHeight={}, viewportHeight={})",
                hasScroll, scrollHeight, viewportHeight);

        return hasScroll;
    }

    /**
     * Capture all viewports needed to cover the full page.
     * Uses native Selenium screenshot for each viewport.
     *
     * @param driver the WebDriver instance
     * @return list of screenshots (1 or more)
     */
    public List<BufferedImage> captureAllViewports(WebDriver driver) {
        List<BufferedImage> screenshots = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Get dimensions
        long scrollHeight = ((Number) js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);")).longValue();
        long viewportHeight = ((Number) js.executeScript("return window.innerHeight;")).longValue();

        int count = calculateScreenshotCount(driver);

        log.info("Capturing {} viewport(s) - page height: {}, viewport height: {}",
                count, scrollHeight, viewportHeight);

        // Scroll to top first
        js.executeScript("window.scrollTo(0, 0);");
        waitForScroll();

        for (int i = 0; i < count; i++) {
            // Calculate scroll position
            long scrollPosition = i * viewportHeight;

            // For the last screenshot, scroll to show the bottom of the page
            if (i == count - 1 && count > 1) {
                scrollPosition = scrollHeight - viewportHeight;
                if (scrollPosition < 0) scrollPosition = 0;
            }

            // Scroll to position
            js.executeScript("window.scrollTo(0, " + scrollPosition + ");");
            waitForScroll();

            // Take screenshot
            BufferedImage screenshot = captureViewport(driver);
            screenshots.add(screenshot);

            log.debug("Captured viewport {} at scroll position {} ({}x{})",
                    i + 1, scrollPosition, screenshot.getWidth(), screenshot.getHeight());
        }

        // Scroll back to top
        js.executeScript("window.scrollTo(0, 0);");

        return screenshots;
    }

    /**
     * Capture exactly N viewports (used when comparing against baselines).
     * This ensures we take the same number of screenshots as the baseline has.
     *
     * @param driver the WebDriver instance
     * @param count number of screenshots to take
     * @return list of screenshots
     */
    public List<BufferedImage> captureViewports(WebDriver driver, int count) {
        List<BufferedImage> screenshots = new ArrayList<>();
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Get dimensions
        long scrollHeight = ((Number) js.executeScript(
                "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);")).longValue();
        long viewportHeight = ((Number) js.executeScript("return window.innerHeight;")).longValue();

        log.info("Capturing {} viewport(s) as requested - page height: {}, viewport height: {}",
                count, scrollHeight, viewportHeight);

        // Scroll to top first
        js.executeScript("window.scrollTo(0, 0);");
        waitForScroll();

        for (int i = 0; i < count; i++) {
            // Calculate scroll position
            long scrollPosition = i * viewportHeight;

            // For the last screenshot, scroll to show the bottom
            if (i == count - 1 && count > 1) {
                scrollPosition = Math.max(0, scrollHeight - viewportHeight);
            }

            // Scroll to position
            js.executeScript("window.scrollTo(0, " + scrollPosition + ");");
            waitForScroll();

            // Take screenshot
            BufferedImage screenshot = captureViewport(driver);
            screenshots.add(screenshot);

            log.debug("Captured viewport {} at scroll position {} ({}x{})",
                    i + 1, scrollPosition, screenshot.getWidth(), screenshot.getHeight());
        }

        // Scroll back to top
        js.executeScript("window.scrollTo(0, 0);");

        return screenshots;
    }

    /**
     * Capture single viewport using native Selenium screenshot.
     * This captures exactly what is visible in the browser window.
     *
     * @param driver the WebDriver instance
     * @return the captured image
     */
    public BufferedImage captureViewport(WebDriver driver) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] screenshotBytes = ts.getScreenshotAs(OutputType.BYTES);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));

            log.debug("Captured viewport: {}x{}", image.getWidth(), image.getHeight());
            return image;

        } catch (IOException e) {
            throw new RuntimeException("Failed to capture viewport screenshot", e);
        }
    }

    /**
     * Take viewport screenshot (visible area only).
     * Uses native Selenium screenshot.
     */
    public Screenshot takeViewportScreenshot(WebDriver driver) {
        log.debug("Taking viewport screenshot");
        BufferedImage image = captureViewport(driver);
        return new Screenshot(image);
    }

    /**
     * Take full page screenshot.
     * For single-viewport pages, returns viewport screenshot.
     * For scrollable pages, captures first viewport only (use captureAllViewports for all).
     */
    public Screenshot takeFullPageScreenshot(WebDriver driver) {
        log.debug("Taking full page screenshot (first viewport)");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollTo(0, 0);");
        waitForScroll();

        return takeViewportScreenshot(driver);
    }

    /**
     * Take element screenshot.
     */
    public Screenshot takeElementScreenshot(WebDriver driver, WebElement element) {
        log.debug("Taking element screenshot");
        return elementAshot.takeScreenshot(driver, element);
    }

    /**
     * Take element screenshot by locator.
     */
    public Screenshot takeElementScreenshot(WebDriver driver, By locator) {
        WebElement element = driver.findElement(locator);
        return takeElementScreenshot(driver, element);
    }

    /**
     * Wait for scroll animation to complete.
     */
    private void waitForScroll() {
        try {
            Thread.sleep(SCROLL_WAIT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Save screenshot to file.
     */
    public File saveScreenshot(Screenshot screenshot, String filePath) throws IOException {
        Path path = Path.of(filePath);
        Files.createDirectories(path.getParent());
        File file = path.toFile();
        ImageIO.write(screenshot.getImage(), "PNG", file);
        log.info("Screenshot saved to: {}", filePath);
        return file;
    }

    /**
     * Save screenshot to file.
     */
    public File saveScreenshot(Screenshot screenshot, Path filePath) throws IOException {
        return saveScreenshot(screenshot, filePath.toString());
    }

    /**
     * Save BufferedImage to file.
     */
    public File saveImage(BufferedImage image, Path filePath) throws IOException {
        Files.createDirectories(filePath.getParent());
        File file = filePath.toFile();
        ImageIO.write(image, "PNG", file);
        log.debug("Image saved to: {}", filePath);
        return file;
    }

    /**
     * Convert screenshot to byte array.
     */
    public byte[] toByteArray(Screenshot screenshot) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(screenshot.getImage(), "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Convert byte array to BufferedImage.
     */
    public BufferedImage toBufferedImage(byte[] imageBytes) throws IOException {
        return ImageIO.read(new ByteArrayInputStream(imageBytes));
    }

    /**
     * Load image from file.
     */
    public BufferedImage loadImage(String filePath) throws IOException {
        return ImageIO.read(new File(filePath));
    }

    /**
     * Load image from file.
     */
    public BufferedImage loadImage(Path filePath) throws IOException {
        return ImageIO.read(filePath.toFile());
    }

    /**
     * Create Screenshot object from BufferedImage.
     */
    public Screenshot createScreenshot(BufferedImage image) {
        return new Screenshot(image);
    }

    /**
     * Create Screenshot object from file.
     */
    public Screenshot createScreenshot(String filePath) throws IOException {
        BufferedImage image = loadImage(filePath);
        return createScreenshot(image);
    }

    /**
     * Take screenshot with retry on failure.
     */
    public Screenshot takeScreenshotWithRetry(WebDriver driver, ScreenshotType type, int maxRetries) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return takeScreenshot(driver, type);
            } catch (Exception e) {
                lastException = e;
                attempt++;
                log.warn("Screenshot attempt {} failed: {}", attempt, e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        throw new RuntimeException("Failed to take screenshot after " + maxRetries + " attempts", lastException);
    }

    /**
     * Get image dimensions.
     */
    public int[] getImageDimensions(BufferedImage image) {
        return new int[]{image.getWidth(), image.getHeight()};
    }

    /**
     * Resize image to specified dimensions.
     */
    public BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }
}
