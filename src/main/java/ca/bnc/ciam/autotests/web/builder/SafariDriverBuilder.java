package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for Safari WebDriver instances.
 * Note: Safari driver is only available on macOS.
 */
@Slf4j
public class SafariDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        SafariOptions options = createOptions(config);

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            return buildLocal(options, config);
        } else {
            return buildRemote(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "safari";
    }

    /**
     * Create Safari options based on configuration.
     * Note: Safari has limited configuration options compared to other browsers.
     */
    private SafariOptions createOptions(WebConfig config) {
        SafariOptions options = new SafariOptions();

        // Safari Technology Preview (for testing newer features)
        // options.setUseTechnologyPreview(true);

        return options;
    }

    /**
     * Build local Safari driver.
     */
    private WebDriver buildLocal(SafariOptions options, WebConfig config) {
        log.info("Building local Safari driver");

        // Check if running on macOS
        String os = System.getProperty("os.name").toLowerCase();
        if (!os.contains("mac")) {
            throw new UnsupportedOperationException(
                    "Safari WebDriver is only supported on macOS. Current OS: " + os);
        }

        // Note: Safari driver is bundled with Safari on macOS
        // Ensure 'Allow Remote Automation' is enabled in Safari's Develop menu

        SafariDriver driver = new SafariDriver(options);
        configureTimeouts(driver, config);

        if (config.isMaximizeWindow()) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    /**
     * Build remote Safari driver (SauceLabs).
     */
    private WebDriver buildRemote(SafariOptions options, WebConfig config) {
        log.info("Building remote Safari driver for SauceLabs");

        // SauceLabs capabilities
        Map<String, Object> sauceOptions = new HashMap<>();
        sauceOptions.put("username", config.getSauceUsername());
        sauceOptions.put("accessKey", config.getSauceAccessKey());
        sauceOptions.put("name", config.getTestName());
        sauceOptions.put("build", config.getBuildName());

        options.setCapability("platformName", "macOS 13"); // Safari requires macOS
        options.setCapability("browserVersion", config.getBrowserVersion());
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
