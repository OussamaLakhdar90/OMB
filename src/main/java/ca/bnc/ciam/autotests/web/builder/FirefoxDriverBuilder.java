package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for Firefox WebDriver instances.
 */
@Slf4j
public class FirefoxDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        FirefoxOptions options = createOptions(config);

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            return buildLocal(options, config);
        } else {
            return buildRemote(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "firefox";
    }

    /**
     * Create Firefox options based on configuration.
     */
    private FirefoxOptions createOptions(WebConfig config) {
        FirefoxOptions options = new FirefoxOptions();

        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("-headless");
        }

        // Window size
        if (!config.isMaximizeWindow()) {
            options.addArguments(String.format("-width=%d", config.getWindowWidth()));
            options.addArguments(String.format("-height=%d", config.getWindowHeight()));
        }

        // Accept insecure certs
        if (config.isAcceptInsecureCerts()) {
            options.setAcceptInsecureCerts(true);
        }

        // Custom binary path
        if (config.getBrowserBinaryPath() != null && !config.getBrowserBinaryPath().isEmpty()) {
            options.setBinary(config.getBrowserBinaryPath());
        }

        // Firefox profile preferences
        FirefoxProfile profile = new FirefoxProfile();
        profile.setPreference("dom.webnotifications.enabled", false);
        profile.setPreference("dom.push.enabled", false);
        profile.setPreference("geo.enabled", false);
        profile.setPreference("browser.download.folderList", 2);
        profile.setPreference("browser.helperApps.neverAsk.saveToDisk",
                "application/pdf,application/zip,application/octet-stream");

        options.setProfile(profile);

        return options;
    }

    /**
     * Build local Firefox driver.
     */
    private WebDriver buildLocal(FirefoxOptions options, WebConfig config) {
        log.info("Building local Firefox driver (headless: {})", config.isHeadless());

        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            System.setProperty("webdriver.gecko.driver", config.getDriverPath());
        }

        FirefoxDriver driver = new FirefoxDriver(options);
        configureTimeouts(driver, config);

        if (config.isMaximizeWindow()) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    /**
     * Build remote Firefox driver (SauceLabs).
     */
    private WebDriver buildRemote(FirefoxOptions options, WebConfig config) {
        log.info("Building remote Firefox driver for SauceLabs");

        // SauceLabs capabilities
        Map<String, Object> sauceOptions = new HashMap<>();
        sauceOptions.put("username", config.getSauceUsername());
        sauceOptions.put("accessKey", config.getSauceAccessKey());
        sauceOptions.put("name", config.getTestName());
        sauceOptions.put("build", config.getBuildName());

        options.setCapability("platformName", config.getPlatform());
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
