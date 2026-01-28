package ca.bnc.ciam.autotests.visual;

import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.HasDevTools;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.HasFullPageScreenshot;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Map;
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
     * Uses native methods for each browser:
     * - Firefox: HasFullPageScreenshot interface
     * - Chrome/Edge: CDP Page.captureScreenshot with captureBeyondViewport
     * - Others: AShot with scaling fallback
     */
    public Screenshot takeFullPageScreenshot(WebDriver driver) {
        log.debug("Taking full page screenshot");

        // Firefox supports native full page screenshot
        if (driver instanceof HasFullPageScreenshot) {
            try {
                byte[] screenshotBytes = ((HasFullPageScreenshot) driver).getFullPageScreenshotAs(OutputType.BYTES);
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
                log.debug("Full page screenshot captured using Firefox native ({}x{})",
                        image.getWidth(), image.getHeight());
                return new Screenshot(image);
            } catch (Exception e) {
                log.warn("Firefox full page screenshot failed, falling back to AShot: {}", e.getMessage());
            }
        }

        // Chrome/Edge: Use CDP for full page screenshot
        if (driver instanceof ChromiumDriver) {
            try {
                Screenshot cdpScreenshot = takeCdpFullPageScreenshot((ChromiumDriver) driver);
                if (cdpScreenshot != null) {
                    return cdpScreenshot;
                }
            } catch (Exception e) {
                log.warn("CDP full page screenshot failed, falling back to AShot: {}", e.getMessage());
            }
        }

        // Fallback: AShot with proper scaling
        float dpr = getDevicePixelRatio(driver);
        log.debug("Device pixel ratio: {}", dpr);

        AShot screenshotAshot;
        if (dpr > 1) {
            log.debug("Using scaled shooting strategy for DPR: {}", dpr);
            screenshotAshot = new AShot()
                    .shootingStrategy(ShootingStrategies.viewportPasting(
                            ShootingStrategies.scaling(dpr), DEFAULT_SCROLL_TIMEOUT))
                    .coordsProvider(new WebDriverCoordsProvider());
        } else {
            screenshotAshot = fullPageAshot;
        }

        return screenshotAshot.takeScreenshot(driver);
    }

    /**
     * Take full page screenshot using Chrome DevTools Protocol.
     * Works for Chrome and Edge browsers.
     */
    private Screenshot takeCdpFullPageScreenshot(ChromiumDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Get full page dimensions
            Map<String, Object> metrics = getPageMetrics(js);
            long pageWidth = ((Number) metrics.get("width")).longValue();
            long pageHeight = ((Number) metrics.get("height")).longValue();
            float dpr = ((Number) metrics.get("devicePixelRatio")).floatValue();

            log.debug("Page dimensions: {}x{}, DPR: {}", pageWidth, pageHeight, dpr);

            // Use CDP to capture full page
            DevTools devTools = driver.getDevTools();
            devTools.createSession();

            // Set device metrics to full page size
            Map<String, Object> deviceMetrics = new java.util.HashMap<>();
            deviceMetrics.put("width", pageWidth);
            deviceMetrics.put("height", pageHeight);
            deviceMetrics.put("deviceScaleFactor", dpr);
            deviceMetrics.put("mobile", false);

            driver.executeCdpCommand("Emulation.setDeviceMetricsOverride", deviceMetrics);

            // Capture screenshot
            Map<String, Object> screenshotParams = new java.util.HashMap<>();
            screenshotParams.put("captureBeyondViewport", true);
            screenshotParams.put("fromSurface", true);

            Map<String, Object> result = driver.executeCdpCommand("Page.captureScreenshot", screenshotParams);
            String base64Screenshot = (String) result.get("data");

            // Clear device metrics override
            driver.executeCdpCommand("Emulation.clearDeviceMetricsOverride", java.util.Collections.emptyMap());

            // Convert to BufferedImage
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Screenshot);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

            log.debug("CDP full page screenshot captured ({}x{})", image.getWidth(), image.getHeight());
            return new Screenshot(image);

        } catch (Exception e) {
            log.debug("CDP screenshot failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get page metrics using JavaScript.
     */
    private Map<String, Object> getPageMetrics(JavascriptExecutor js) {
        String script = """
            return {
                width: Math.max(
                    document.body.scrollWidth,
                    document.documentElement.scrollWidth,
                    document.body.offsetWidth,
                    document.documentElement.offsetWidth,
                    document.body.clientWidth,
                    document.documentElement.clientWidth
                ),
                height: Math.max(
                    document.body.scrollHeight,
                    document.documentElement.scrollHeight,
                    document.body.offsetHeight,
                    document.documentElement.offsetHeight,
                    document.body.clientHeight,
                    document.documentElement.clientHeight
                ),
                devicePixelRatio: window.devicePixelRatio || 1
            };
            """;

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) js.executeScript(script);
        return result;
    }

    /**
     * Get device pixel ratio from browser.
     */
    private float getDevicePixelRatio(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript("return window.devicePixelRatio || 1;");
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception e) {
            log.debug("Could not get device pixel ratio: {}", e.getMessage());
        }
        return 1.0f;
    }

    /**
     * Take viewport screenshot (visible area only).
     * Uses native Selenium screenshot which captures the full viewport width.
     */
    public Screenshot takeViewportScreenshot(WebDriver driver) {
        log.debug("Taking viewport screenshot");
        try {
            // Use native Selenium screenshot - captures full viewport correctly
            TakesScreenshot ts = (TakesScreenshot) driver;
            byte[] screenshotBytes = ts.getScreenshotAs(OutputType.BYTES);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
            log.debug("Viewport screenshot captured ({}x{})", image.getWidth(), image.getHeight());
            return new Screenshot(image);
        } catch (Exception e) {
            log.warn("Native viewport screenshot failed, falling back to AShot: {}", e.getMessage());
            return ashot.takeScreenshot(driver);
        }
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
