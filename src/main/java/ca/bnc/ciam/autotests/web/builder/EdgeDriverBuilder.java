package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for Microsoft Edge WebDriver instances.
 */
@Slf4j
public class EdgeDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        EdgeOptions options = createOptions(config);

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            return buildLocal(options, config);
        } else {
            return buildRemote(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "edge";
    }

    /**
     * Create Edge options based on configuration.
     */
    private EdgeOptions createOptions(WebConfig config) {
        EdgeOptions options = new EdgeOptions();

        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }

        // Common arguments (Edge uses Chromium)
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-infobars");
        options.addArguments("--disable-notifications");
        options.addArguments("--disable-popup-blocking");

        // Window size
        if (!config.isMaximizeWindow()) {
            options.addArguments(String.format("--window-size=%d,%d",
                    config.getWindowWidth(), config.getWindowHeight()));
        }

        // Accept insecure certs
        if (config.isAcceptInsecureCerts()) {
            options.setAcceptInsecureCerts(true);
        }

        // Custom binary path
        if (config.getBrowserBinaryPath() != null && !config.getBrowserBinaryPath().isEmpty()) {
            options.setBinary(config.getBrowserBinaryPath());
        }

        // Enable DevTools
        if (config.isEnableDevTools()) {
            options.addArguments("--auto-open-devtools-for-tabs");
        }

        // Preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        options.setExperimentalOption("prefs", prefs);

        // Exclude automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    /**
     * Build local Edge driver.
     */
    private WebDriver buildLocal(EdgeOptions options, WebConfig config) {
        log.info("Building local Edge driver (headless: {})", config.isHeadless());

        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            System.setProperty("webdriver.edge.driver", config.getDriverPath());
        }

        EdgeDriver driver = new EdgeDriver(options);
        configureTimeouts(driver, config);

        if (config.isMaximizeWindow()) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    /**
     * Build remote Edge driver (SauceLabs).
     */
    private WebDriver buildRemote(EdgeOptions options, WebConfig config) {
        log.info("Building remote Edge driver for SauceLabs");

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
