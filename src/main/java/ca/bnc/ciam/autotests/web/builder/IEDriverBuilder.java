package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for Internet Explorer WebDriver instances.
 * Note: IE driver is deprecated and only available on Windows.
 * Consider using Edge in IE mode instead.
 */
@Slf4j
public class IEDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        InternetExplorerOptions options = createOptions(config);

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            return buildLocal(options, config);
        } else {
            return buildRemote(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "ie";
    }

    /**
     * Create IE options based on configuration.
     */
    private InternetExplorerOptions createOptions(WebConfig config) {
        InternetExplorerOptions options = new InternetExplorerOptions();

        // Common IE settings for stability
        options.ignoreZoomSettings();
        options.introduceFlakinessByIgnoringSecurityDomains();
        options.enablePersistentHovering();

        // Accept insecure certs
        if (config.isAcceptInsecureCerts()) {
            options.setCapability("acceptInsecureCerts", true);
        }

        return options;
    }

    /**
     * Build local IE driver.
     */
    private WebDriver buildLocal(InternetExplorerOptions options, WebConfig config) {
        log.info("Building local Internet Explorer driver");
        log.warn("IE WebDriver is deprecated. Consider using Edge in IE mode.");

        // Check if running on Windows
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("win")) {
            throw new UnsupportedOperationException(
                    "Internet Explorer WebDriver is only supported on Windows. Current OS: " + os);
        }

        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            System.setProperty("webdriver.ie.driver", config.getDriverPath());
        }

        InternetExplorerDriver driver = new InternetExplorerDriver(options);
        configureTimeouts(driver, config);

        if (config.isMaximizeWindow()) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    /**
     * Build remote IE driver (SauceLabs).
     */
    private WebDriver buildRemote(InternetExplorerOptions options, WebConfig config) {
        log.info("Building remote IE driver for SauceLabs");
        log.warn("IE WebDriver is deprecated. Consider using Edge in IE mode.");

        // SauceLabs capabilities
        Map<String, Object> sauceOptions = new HashMap<>();
        sauceOptions.put("username", config.getSauceUsername());
        sauceOptions.put("accessKey", config.getSauceAccessKey());
        sauceOptions.put("name", config.getTestName());
        sauceOptions.put("build", config.getBuildName());

        options.setCapability("platformName", "Windows 10"); // IE requires Windows
        options.setCapability("browserVersion", "11"); // IE 11 is the last version
        options.setCapability("sauce:options", sauceOptions);

        try {
            RemoteWebDriver driver = new RemoteWebDriver(new URL(config.getSauceLabsUrl()), options);
            configureTimeouts(driver, config);

            if (config.isMaximizeWindow()) {
                driver.manage().window().maximize();
            }

            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid SauceLabs URL", e);
        }
    }

    /**
     * Configure driver timeouts.
     */
    private void configureTimeouts(WebDriver driver, WebConfig config) {
        driver.manage().timeouts().implicitlyWait(config.getImplicitWait());
        driver.manage().timeouts().pageLoadTimeout(config.getPageLoadTimeout());
        driver.manage().timeouts().scriptTimeout(config.getScriptTimeout());
    }
}
