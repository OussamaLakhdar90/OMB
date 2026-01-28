package ca.bnc.ciam.autotests.unit.visual;

import ca.bnc.ciam.autotests.visual.ScreenshotManager;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import ru.yandex.qatools.ashot.Screenshot;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScreenshotManager.
 * Tests screenshot capture functionality with mocked WebDriver.
 */
@Test(groups = "unit")
public class ScreenshotManagerTest {

    private ScreenshotManager screenshotManager;
    private WebDriver mockDriver;
    private WebDriver.Options mockOptions;
    private WebDriver.Window mockWindow;
    private JavascriptExecutor mockJs;

    @BeforeMethod
    public void setUp() {
        screenshotManager = new ScreenshotManager();

        // Create mock that implements both WebDriver and JavascriptExecutor
        mockDriver = mock(WebDriver.class, withSettings().extraInterfaces(
                JavascriptExecutor.class, TakesScreenshot.class));
        mockOptions = mock(WebDriver.Options.class);
        mockWindow = mock(WebDriver.Window.class);
        mockJs = (JavascriptExecutor) mockDriver;

        when(mockDriver.manage()).thenReturn(mockOptions);
        when(mockOptions.window()).thenReturn(mockWindow);
    }

    // ===========================================
    // Default Resolution Tests
    // ===========================================

    @Test
    public void testDefaultWidth() {
        assertThat(ScreenshotManager.DEFAULT_WIDTH).isEqualTo(1920);
    }

    @Test
    public void testDefaultHeight() {
        assertThat(ScreenshotManager.DEFAULT_HEIGHT).isEqualTo(1080);
    }

    // ===========================================
    // ensureStandardResolution Tests
    // ===========================================

    @Test
    public void testEnsureStandardResolution_WhenDifferentSize_ResizesWindow() {
        Dimension originalSize = new Dimension(1280, 720);
        when(mockWindow.getSize()).thenReturn(originalSize);

        Dimension result = screenshotManager.ensureStandardResolution(mockDriver);

        assertThat(result).isEqualTo(originalSize);
        verify(mockWindow).setSize(new Dimension(1920, 1080));
    }

    @Test
    public void testEnsureStandardResolution_WhenAlreadyStandard_DoesNotResize() {
        Dimension standardSize = new Dimension(1920, 1080);
        when(mockWindow.getSize()).thenReturn(standardSize);

        Dimension result = screenshotManager.ensureStandardResolution(mockDriver);

        assertThat(result).isEqualTo(standardSize);
        verify(mockWindow, never()).setSize(any(Dimension.class));
    }

    // ===========================================
    // calculateScreenshotCount Tests
    // ===========================================

    @Test
    public void testCalculateScreenshotCount_SingleViewport() {
        // Page fits in one viewport
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(800L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        int count = screenshotManager.calculateScreenshotCount(mockDriver);

        assertThat(count).isEqualTo(1);
    }

    @Test
    public void testCalculateScreenshotCount_TwoViewports() {
        // Page needs 2 viewports
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(1500L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        int count = screenshotManager.calculateScreenshotCount(mockDriver);

        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testCalculateScreenshotCount_ThreeViewports() {
        // Page needs 3 viewports
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(3000L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        int count = screenshotManager.calculateScreenshotCount(mockDriver);

        assertThat(count).isEqualTo(3);
    }

    @Test
    public void testCalculateScreenshotCount_ExactlyTwoViewports() {
        // Page is exactly 2 viewports tall
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(2160L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        int count = screenshotManager.calculateScreenshotCount(mockDriver);

        assertThat(count).isEqualTo(2);
    }

    @Test
    public void testCalculateScreenshotCount_InvalidViewportHeight_ReturnsOne() {
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(1000L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(0L);

        int count = screenshotManager.calculateScreenshotCount(mockDriver);

        assertThat(count).isEqualTo(1);
    }

    // ===========================================
    // hasVerticalScroll Tests
    // ===========================================

    @Test
    public void testHasVerticalScroll_WhenPageFits_ReturnsFalse() {
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(800L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        boolean hasScroll = screenshotManager.hasVerticalScroll(mockDriver);

        assertThat(hasScroll).isFalse();
    }

    @Test
    public void testHasVerticalScroll_WhenPageTaller_ReturnsTrue() {
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(2000L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        boolean hasScroll = screenshotManager.hasVerticalScroll(mockDriver);

        assertThat(hasScroll).isTrue();
    }

    @Test
    public void testHasVerticalScroll_WhenExactlyEqual_ReturnsFalse() {
        when(mockJs.executeScript("return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight);"))
                .thenReturn(1080L);
        when(mockJs.executeScript("return window.innerHeight;"))
                .thenReturn(1080L);

        boolean hasScroll = screenshotManager.hasVerticalScroll(mockDriver);

        assertThat(hasScroll).isFalse();
    }

    // ===========================================
    // captureViewport Tests
    // ===========================================

    @Test
    public void testCaptureViewport_ReturnsBufferedImage() throws IOException {
        // Create a test image
        BufferedImage testImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        byte[] imageBytes = imageToBytes(testImage);

        TakesScreenshot mockTs = (TakesScreenshot) mockDriver;
        when(mockTs.getScreenshotAs(OutputType.BYTES)).thenReturn(imageBytes);

        BufferedImage result = screenshotManager.captureViewport(mockDriver);

        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(1920);
        assertThat(result.getHeight()).isEqualTo(1080);
    }

    // ===========================================
    // takeScreenshot Tests
    // ===========================================

    @Test
    public void testTakeScreenshot_ViewportType() throws IOException {
        BufferedImage testImage = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
        byte[] imageBytes = imageToBytes(testImage);
        TakesScreenshot mockTs = (TakesScreenshot) mockDriver;
        when(mockTs.getScreenshotAs(OutputType.BYTES)).thenReturn(imageBytes);

        Screenshot result = screenshotManager.takeScreenshot(mockDriver, ScreenshotType.VIEWPORT);

        assertThat(result).isNotNull();
        assertThat(result.getImage().getWidth()).isEqualTo(1920);
    }

    @Test
    public void testTakeScreenshot_ElementTypeWithoutElement_ThrowsException() {
        assertThatThrownBy(() -> screenshotManager.takeScreenshot(mockDriver, ScreenshotType.ELEMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Element type requires element parameter");
    }

    // ===========================================
    // Image Utility Tests
    // ===========================================

    @Test
    public void testGetImageDimensions() {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);

        int[] dimensions = screenshotManager.getImageDimensions(image);

        assertThat(dimensions).containsExactly(800, 600);
    }

    @Test
    public void testResizeImage() {
        BufferedImage original = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);

        BufferedImage resized = screenshotManager.resizeImage(original, 960, 540);

        assertThat(resized.getWidth()).isEqualTo(960);
        assertThat(resized.getHeight()).isEqualTo(540);
    }

    @Test
    public void testCreateScreenshot_FromBufferedImage() {
        BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);

        Screenshot screenshot = screenshotManager.createScreenshot(image);

        assertThat(screenshot).isNotNull();
        assertThat(screenshot.getImage()).isEqualTo(image);
    }

    @Test
    public void testSaveImage() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Path tempFile = Files.createTempFile("test_screenshot", ".png");

        try {
            screenshotManager.saveImage(image, tempFile);

            assertThat(Files.exists(tempFile)).isTrue();
            assertThat(Files.size(tempFile)).isGreaterThan(0);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testToByteArray() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Screenshot screenshot = new Screenshot(image);

        byte[] bytes = screenshotManager.toByteArray(screenshot);

        assertThat(bytes).isNotNull();
        assertThat(bytes.length).isGreaterThan(0);
    }

    @Test
    public void testToBufferedImage() throws IOException {
        BufferedImage original = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        byte[] bytes = imageToBytes(original);

        BufferedImage result = screenshotManager.toBufferedImage(bytes);

        assertThat(result).isNotNull();
        assertThat(result.getWidth()).isEqualTo(100);
        assertThat(result.getHeight()).isEqualTo(100);
    }

    // ===========================================
    // Helper Methods
    // ===========================================

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
