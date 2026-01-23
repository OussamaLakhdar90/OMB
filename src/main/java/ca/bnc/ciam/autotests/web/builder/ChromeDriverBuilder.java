package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Builder for Chrome WebDriver instances.
 */
@Slf4j
public class ChromeDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        ChromeOptions options = createOptions(config);

        if (config.getExecutionMode() == ExecutionMode.LOCAL) {
            return buildLocal(options, config);
        } else {
            return buildRemote(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "chrome";
    }

    /**
     * Create Chrome options based on configuration.
     */
    private ChromeOptions createOptions(WebConfig config) {
        ChromeOptions options = new ChromeOptions();

        // Headless mode
        if (config.isHeadless()) {
            options.addArguments("--headless=new");
        }

        // Common arguments for stability
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

        // Preferences - disable password manager and autofill popups
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        // Disable password manager completely
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        // Disable autofill
        prefs.put("autofill.profile_enabled", false);
        prefs.put("autofill.credit_card_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        // Exclude automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    /**
     * Build local Chrome driver.
     */
    private WebDriver buildLocal(ChromeOptions options, WebConfig config) {
        log.info("Building local Chrome driver (headless: {})", config.isHeadless());

        if (config.getDriverPath() != null && !config.getDriverPath().isEmpty()) {
            System.setProperty("webdriver.chrome.driver", config.getDriverPath());
        }

        ChromeDriver driver = new ChromeDriver(options);
        configureTimeouts(driver, config);

        if (config.isMaximizeWindow()) {
            driver.manage().window().maximize();
        }

        return driver;
    }

    /**
     * Build remote Chrome driver (SauceLabs).
     */
    private WebDriver buildRemote(ChromeOptions options, WebConfig config) {
        log.info("Building remote Chrome driver for SauceLabs");

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
