package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.BrowserConfigLoader;
import ca.bnc.ciam.autotests.web.config.BrowserConfigLoader.BrowserConfig;
import ca.bnc.ciam.autotests.web.config.ExecutionMode;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builder for Chrome WebDriver instances.
 */
@Slf4j
public class ChromeDriverBuilder implements IWebDriverBuilder {

    @Override
    public WebDriver build(WebConfig config) {
        ChromeOptions options = createOptions(config);

        // Check if hub mode is enabled via system property (local SauceLabs execution)
        boolean hubModeEnabled = "true".equalsIgnoreCase(System.getProperty("bnc.test.hub.use"));

        if (hubModeEnabled || config.getExecutionMode() == ExecutionMode.SAUCELABS) {
            return buildRemote(options, config);
        } else {
            return buildLocal(options, config);
        }
    }

    @Override
    public String getBrowserName() {
        return "chrome";
    }

    /**
     * Create Chrome options based on configuration.
     * Loads additional options from BrowserConfigLoader (debug_config.json or bnc.web.browsers.config).
     */
    private ChromeOptions createOptions(WebConfig config) {
        ChromeOptions options = new ChromeOptions();

        // Load browser config based on execution mode
        BrowserConfig browserConfig = loadBrowserConfig(config);

        // Headless mode - check both WebConfig and BrowserConfig
        if (config.isHeadless() || browserConfig.isHeadless()) {
            options.addArguments("--headless=new");
        }

        // Base arguments for stability (always included)
        List<String> baseArgs = new ArrayList<>();
        baseArgs.add("--no-sandbox");
        baseArgs.add("--disable-dev-shm-usage");
        baseArgs.add("--disable-gpu");
        baseArgs.add("--disable-extensions");
        baseArgs.add("--disable-infobars");
        // Disable password manager popup completely (Chrome 120+)
        baseArgs.add("--disable-save-password-bubble");
        baseArgs.add("--disable-features=PasswordManager,PasswordLeakDetection,PasswordSaving,PasswordGeneration");
        // Use guest mode for clean session without password prompts
        baseArgs.add("--guest");

        // Add base args
        for (String arg : baseArgs) {
            options.addArguments(arg);
        }

        // Add args from BrowserConfig (from JSON file)
        if (browserConfig.hasChromeArgs()) {
            for (String arg : browserConfig.getChromeArgs()) {
                if (!baseArgs.contains(arg)) { // Avoid duplicates
                    options.addArguments(arg);
                    log.debug("Added Chrome arg from config: {}", arg);
                }
            }
        } else {
            // Fallback default args if no config
            options.addArguments("--disable-notifications");
            options.addArguments("--disable-popup-blocking");
        }

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

        // Preferences - base prefs always included, then merge from config
        Map<String, Object> prefs = new HashMap<>();

        // Base prefs (always included for stability)
        prefs.put("profile.default_content_setting_values.notifications", 2);
        // Password manager prefs (always disabled for automation)
        prefs.put("credentials_enable_service", false);
        prefs.put("credentials_enable_autosign_in", false);
        prefs.put("profile.password_manager_enabled", false);
        prefs.put("profile.password_manager_leak_detection", false);
        prefs.put("password_manager.enabled", false);
        // Autofill prefs (always disabled for automation)
        prefs.put("autofill.profile_enabled", false);
        prefs.put("autofill.credit_card_enabled", false);

        // Add/override prefs from BrowserConfig (from JSON file)
        if (browserConfig.hasChromePrefs()) {
            prefs.putAll(browserConfig.getChromePrefs());
            log.debug("Added Chrome prefs from config: {}", browserConfig.getChromePrefs().keySet());
        }
        options.setExperimentalOption("prefs", prefs);

        // Exclude automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
        options.setExperimentalOption("useAutomationExtension", false);

        return options;
    }

    /**
     * Load browser configuration based on execution mode.
     * When hub mode is enabled (bnc.test.hub.use=true) and browser config is specified,
     * the browser config file overrides local settings.
     */
    private BrowserConfig loadBrowserConfig(WebConfig config) {
        BrowserConfigLoader loader = BrowserConfigLoader.getInstance();

        // Check if hub mode is enabled via system property
        boolean hubModeEnabled = "true".equalsIgnoreCase(System.getProperty("bnc.test.hub.use"));

        // Get browser config path from WebConfig or system property
        String configPath = config.getBrowserConfigPath();
        if (configPath == null || configPath.isEmpty()) {
            configPath = System.getProperty("bnc.web.browsers.config");
        }

        // If hub mode is enabled and browser config is specified, load from config file
        // This allows local execution to use SauceLabs with a specific browser config
        if (hubModeEnabled && configPath != null && !configPath.isEmpty()) {
            log.info("Hub mode enabled - loading browser config from: {}", configPath);
            return loader.loadPipelineConfig(configPath);
        }

        // Standard logic based on execution mode
        if (config.getExecutionMode() == ExecutionMode.LOCAL && !hubModeEnabled) {
            // Local mode - load from debug_config.json
            return loader.loadLocalConfig();
        } else {
            // Pipeline/SauceLabs mode - load from bnc.web.browsers.config
            if (configPath != null && !configPath.isEmpty()) {
                return loader.loadPipelineConfig(configPath);
            }
            return BrowserConfig.defaults();
        }
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
     * Build remote Chrome driver (SauceLabs/Pipeline).
     */
    private WebDriver buildRemote(ChromeOptions options, WebConfig config) {
        log.info("Building remote Chrome driver for SauceLabs/Pipeline");

        // Load browser config for sauce:options
        BrowserConfig browserConfig = loadBrowserConfig(config);

        // Build sauce:options
        Map<String, Object> sauceOptions = new HashMap<>();

        // Start with sauce:options from browser config file if available
        if (browserConfig.hasSauceOptions()) {
            sauceOptions.putAll(browserConfig.getSauceOptions());
            log.info("Loaded sauce:options from browser config: {}", browserConfig.getSauceOptions().keySet());
        }

        // Add/override with WebConfig values
        if (config.getSauceUsername() != null) {
            sauceOptions.put("username", config.getSauceUsername());
        }
        if (config.getSauceAccessKey() != null) {
            sauceOptions.put("accessKey", config.getSauceAccessKey());
        }
        if (config.getTestName() != null) {
            sauceOptions.put("name", config.getTestName());
        }
        if (config.getBuildName() != null) {
            sauceOptions.put("build", config.getBuildName());
        }

        // Tunnel configuration - priority: system property > WebConfig > BrowserConfig
        String tunnelIdentifier = System.getProperty("bnc.test.hub.tunnelIdentifier");
        if (tunnelIdentifier == null || tunnelIdentifier.isEmpty()) {
            tunnelIdentifier = config.getTunnelName();
        }
        if (tunnelIdentifier == null || tunnelIdentifier.isEmpty()) {
            tunnelIdentifier = browserConfig.getTunnelIdentifier();
        }
        if (tunnelIdentifier != null && !tunnelIdentifier.isEmpty()) {
            sauceOptions.put("tunnelIdentifier", tunnelIdentifier);
            log.info("Using tunnel: {}", tunnelIdentifier);
        }

        String parentTunnel = System.getProperty("bnc.test.hub.parentTunnel");
        if (parentTunnel == null || parentTunnel.isEmpty()) {
            parentTunnel = config.getTunnelOwner();
        }
        if (parentTunnel == null || parentTunnel.isEmpty()) {
            parentTunnel = browserConfig.getParentTunnel();
        }
        if (parentTunnel != null && !parentTunnel.isEmpty()) {
            sauceOptions.put("parentTunnel", parentTunnel);
            log.info("Using parent tunnel: {}", parentTunnel);
        }

        // Other sauce options with fallbacks
        if (!sauceOptions.containsKey("extendedDebugging")) {
            sauceOptions.put("extendedDebugging", config.isExtendedDebugging());
        }
        if (!sauceOptions.containsKey("screenResolution")) {
            sauceOptions.put("screenResolution", config.getScreenResolution());
        }
        if (!sauceOptions.containsKey("idleTimeout")) {
            sauceOptions.put("idleTimeout", config.getIdleTimeout());
        }

        // Platform and browser version from browser config or WebConfig
        String platformName = browserConfig.getPlatformName() != null ?
                browserConfig.getPlatformName() : config.getPlatform();
        String browserVersion = browserConfig.getBrowserVersion() != null ?
                browserConfig.getBrowserVersion() : config.getBrowserVersion();

        options.setCapability("platformName", platformName);
        options.setCapability("browserVersion", browserVersion);
        options.setCapability("sauce:options", sauceOptions);

        // Determine hub URL - priority: system property > WebConfig
        String hubUrl = System.getProperty("bnc.test.hub.url");
        if (hubUrl == null || hubUrl.isEmpty()) {
            hubUrl = config.getSauceLabsUrl();
        }
        log.info("Connecting to remote hub: {}", maskCredentials(hubUrl));
        log.info("Platform: {}, Browser version: {}", platformName, browserVersion);

        try {
            RemoteWebDriver driver = new RemoteWebDriver(new URL(hubUrl), options);
            configureTimeouts(driver, config);

            if (config.isMaximizeWindow()) {
                driver.manage().window().maximize();
            }

            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid SauceLabs/Hub URL: " + hubUrl, e);
        }
    }

    /**
     * Mask credentials in URL for logging.
     */
    private String maskCredentials(String url) {
        if (url == null) return null;
        return url.replaceAll(":[^@]+@", ":****@");
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
