package ca.bnc.ciam.autotests.visual;

import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages screenshot capture using AShot library.
 * Supports full page, viewport, and element screenshots.
 */
@Slf4j
public class ScreenshotManager {

    private static final int DEFAULT_SCROLL_TIMEOUT = 100;
    private static final float DEFAULT_DPR = 2.0f; // Default device pixel ratio

    private final AShot ashot;
    private final AShot fullPageAshot;
    private final AShot elementAshot;

    public ScreenshotManager() {
        // Standard viewport screenshot
        this.ashot = new AShot()
                .coordsProvider(new WebDriverCoordsProvider());

        // Full page screenshot with scrolling
        this.fullPageAshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(DEFAULT_SCROLL_TIMEOUT))
                .coordsProvider(new WebDriverCoordsProvider());

        // Element-specific screenshot
        this.elementAshot = new AShot()
                .coordsProvider(new WebDriverCoordsProvider());
    }

    /**
     * Create manager with custom scroll timeout.
     */
    public ScreenshotManager(int scrollTimeout) {
        this.ashot = new AShot()
                .coordsProvider(new WebDriverCoordsProvider());

        this.fullPageAshot = new AShot()
                .shootingStrategy(ShootingStrategies.viewportPasting(scrollTimeout))
                .coordsProvider(new WebDriverCoordsProvider());

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
            case BOTH -> takeFullPageScreenshot(driver); // Default to full page
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
     * Take full page screenshot (scrolling capture).
     */
    public Screenshot takeFullPageScreenshot(WebDriver driver) {
        log.debug("Taking full page screenshot");
        return fullPageAshot.takeScreenshot(driver);
    }

    /**
     * Take viewport screenshot (visible area only).
     */
    public Screenshot takeViewportScreenshot(WebDriver driver) {
        log.debug("Taking viewport screenshot");
        return ashot.takeScreenshot(driver);
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
        Screenshot screenshot = new Screenshot(image);
        return screenshot;
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
