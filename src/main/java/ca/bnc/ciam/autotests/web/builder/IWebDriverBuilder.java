package ca.bnc.ciam.autotests.web.builder;

import ca.bnc.ciam.autotests.web.config.WebConfig;
import org.openqa.selenium.WebDriver;

/**
 * Interface for WebDriver builders.
 * Each browser type has its own implementation.
 */
public interface IWebDriverBuilder {

    /**
     * Build a WebDriver instance with the given configuration.
     *
     * @param config The web configuration
     * @return A configured WebDriver instance
     */
    WebDriver build(WebConfig config);

    /**
     * Get the browser name this builder handles.
     *
     * @return The browser name
     */
    String getBrowserName();
}
