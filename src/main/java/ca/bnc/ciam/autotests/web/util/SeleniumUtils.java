package ca.bnc.ciam.autotests.web.util;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Utility class for common Selenium operations.
 */
@Slf4j
public final class SeleniumUtils {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private SeleniumUtils() {
        // Utility class - prevent instantiation
    }

    // ==================== Waiting ====================

    /**
     * Wait for a condition to be true.
     */
    public static <T> T waitFor(WebDriver driver, ExpectedCondition<T> condition, Duration timeout) {
        return new WebDriverWait(driver, timeout).until(condition);
    }

    /**
     * Wait for a condition with default timeout.
     */
    public static <T> T waitFor(WebDriver driver, ExpectedCondition<T> condition) {
        return waitFor(driver, condition, DEFAULT_TIMEOUT);
    }

    /**
     * Sleep for specified duration (use sparingly).
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Sleep interrupted", e);
        }
    }

    /**
     * Sleep for specified milliseconds.
     */
    public static void sleep(long milliseconds) {
        sleep(Duration.ofMillis(milliseconds));
    }

    /**
     * Wait for page to be fully loaded.
     */
    public static void waitForPageLoad(WebDriver driver, Duration timeout) {
        new WebDriverWait(driver, timeout).until(
                d -> ((JavascriptExecutor) d).executeScript("return document.readyState").equals("complete"));
    }

    /**
     * Wait for page to be fully loaded with default timeout.
     */
    public static void waitForPageLoad(WebDriver driver) {
        waitForPageLoad(driver, DEFAULT_TIMEOUT);
    }

    // ==================== Element Interactions ====================

    /**
     * Hover over an element.
     */
    public static void hover(WebDriver driver, WebElement element) {
        new Actions(driver).moveToElement(element).perform();
    }

    /**
     * Hover over an element by locator.
     */
    public static void hover(WebDriver driver, By locator) {
        WebElement element = driver.findElement(locator);
        hover(driver, element);
    }

    /**
     * Double-click an element.
     */
    public static void doubleClick(WebDriver driver, WebElement element) {
        new Actions(driver).doubleClick(element).perform();
    }

    /**
     * Right-click (context click) an element.
     */
    public static void rightClick(WebDriver driver, WebElement element) {
        new Actions(driver).contextClick(element).perform();
    }

    /**
     * Drag and drop.
     */
    public static void dragAndDrop(WebDriver driver, WebElement source, WebElement target) {
        new Actions(driver).dragAndDrop(source, target).perform();
    }

    /**
     * Drag and drop by offset.
     */
    public static void dragAndDropBy(WebDriver driver, WebElement element, int xOffset, int yOffset) {
        new Actions(driver).dragAndDropBy(element, xOffset, yOffset).perform();
    }

    /**
     * Click and hold.
     */
    public static void clickAndHold(WebDriver driver, WebElement element) {
        new Actions(driver).clickAndHold(element).perform();
    }

    /**
     * Release click.
     */
    public static void release(WebDriver driver) {
        new Actions(driver).release().perform();
    }

    // ==================== Select/Dropdown ====================

    /**
     * Select option by visible text.
     */
    public static void selectByText(WebElement selectElement, String text) {
        new Select(selectElement).selectByVisibleText(text);
    }

    /**
     * Select option by value.
     */
    public static void selectByValue(WebElement selectElement, String value) {
        new Select(selectElement).selectByValue(value);
    }

    /**
     * Select option by index.
     */
    public static void selectByIndex(WebElement selectElement, int index) {
        new Select(selectElement).selectByIndex(index);
    }

    /**
     * Get all options from a select element.
     */
    public static List<WebElement> getSelectOptions(WebElement selectElement) {
        return new Select(selectElement).getOptions();
    }

    /**
     * Get selected option text.
     */
    public static String getSelectedText(WebElement selectElement) {
        return new Select(selectElement).getFirstSelectedOption().getText();
    }

    /**
     * Get selected option value.
     */
    public static String getSelectedValue(WebElement selectElement) {
        return new Select(selectElement).getFirstSelectedOption().getAttribute("value");
    }

    // ==================== JavaScript ====================

    /**
     * Execute JavaScript and return result.
     */
    public static Object executeScript(WebDriver driver, String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    /**
     * Click element using JavaScript.
     */
    public static void jsClick(WebDriver driver, WebElement element) {
        executeScript(driver, "arguments[0].click();", element);
    }

    /**
     * Set value using JavaScript.
     */
    public static void jsSetValue(WebDriver driver, WebElement element, String value) {
        executeScript(driver, "arguments[0].value = arguments[1];", element, value);
    }

    /**
     * Scroll element into view.
     */
    public static void scrollIntoView(WebDriver driver, WebElement element) {
        executeScript(driver, "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
    }

    /**
     * Scroll to coordinates.
     */
    public static void scrollTo(WebDriver driver, int x, int y) {
        executeScript(driver, "window.scrollTo(arguments[0], arguments[1]);", x, y);
    }

    /**
     * Get page height.
     */
    public static long getPageHeight(WebDriver driver) {
        return (long) executeScript(driver, "return document.body.scrollHeight;");
    }

    /**
     * Highlight element (useful for debugging).
     */
    public static void highlightElement(WebDriver driver, WebElement element) {
        String originalStyle = element.getAttribute("style");
        executeScript(driver,
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                "border: 3px solid red; background: yellow;");
        sleep(500);
        executeScript(driver,
                "arguments[0].setAttribute('style', arguments[1]);",
                element,
                originalStyle != null ? originalStyle : "");
    }

    // ==================== Screenshots ====================

    /**
     * Take screenshot and return as byte array.
     */
    public static byte[] takeScreenshotAsBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /**
     * Take screenshot and return as Base64 string.
     */
    public static String takeScreenshotAsBase64(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }

    /**
     * Take screenshot and save to file.
     */
    public static File takeScreenshot(WebDriver driver, String filePath) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            Path targetPath = Path.of(filePath);
            Files.createDirectories(targetPath.getParent());
            Files.copy(screenshot.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Screenshot saved to: {}", filePath);
            return targetPath.toFile();
        } catch (IOException e) {
            log.error("Failed to save screenshot", e);
            throw new RuntimeException("Failed to save screenshot", e);
        }
    }

    /**
     * Take element screenshot.
     */
    public static byte[] takeElementScreenshot(WebElement element) {
        return element.getScreenshotAs(OutputType.BYTES);
    }

    // ==================== Window/Tab Management ====================

    /**
     * Open new tab and switch to it.
     */
    public static void openNewTab(WebDriver driver) {
        executeScript(driver, "window.open();");
        Set<String> handles = driver.getWindowHandles();
        driver.switchTo().window(handles.toArray(new String[0])[handles.size() - 1]);
    }

    /**
     * Close current tab and switch to previous.
     */
    public static void closeCurrentTab(WebDriver driver) {
        String currentHandle = driver.getWindowHandle();
        Set<String> handles = driver.getWindowHandles();

        driver.close();

        for (String handle : handles) {
            if (!handle.equals(currentHandle)) {
                driver.switchTo().window(handle);
                break;
            }
        }
    }

    /**
     * Get number of open tabs/windows.
     */
    public static int getWindowCount(WebDriver driver) {
        return driver.getWindowHandles().size();
    }

    /**
     * Switch to window by title.
     */
    public static void switchToWindowByTitle(WebDriver driver, String title) {
        String currentHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().contains(title)) {
                return;
            }
        }
        driver.switchTo().window(currentHandle);
        throw new RuntimeException("Window with title containing '" + title + "' not found");
    }

    /**
     * Switch to window by URL.
     */
    public static void switchToWindowByUrl(WebDriver driver, String urlPart) {
        String currentHandle = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getCurrentUrl().contains(urlPart)) {
                return;
            }
        }
        driver.switchTo().window(currentHandle);
        throw new RuntimeException("Window with URL containing '" + urlPart + "' not found");
    }

    // ==================== Cookies ====================

    /**
     * Delete all cookies.
     */
    public static void deleteAllCookies(WebDriver driver) {
        driver.manage().deleteAllCookies();
    }

    /**
     * Delete specific cookie.
     */
    public static void deleteCookie(WebDriver driver, String name) {
        driver.manage().deleteCookieNamed(name);
    }

    /**
     * Get cookie value.
     */
    public static String getCookieValue(WebDriver driver, String name) {
        return driver.manage().getCookieNamed(name).getValue();
    }

    // ==================== Element State Checks ====================

    /**
     * Check if element is clickable.
     */
    public static boolean isClickable(WebDriver driver, By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.elementToBeClickable(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is visible.
     */
    public static boolean isVisible(WebDriver driver, By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if element is present.
     */
    public static boolean isPresent(WebDriver driver, By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    /**
     * Count elements matching locator.
     */
    public static int countElements(WebDriver driver, By locator) {
        return driver.findElements(locator).size();
    }
}
