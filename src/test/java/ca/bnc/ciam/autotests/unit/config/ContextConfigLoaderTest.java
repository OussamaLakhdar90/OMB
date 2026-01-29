package ca.bnc.ciam.autotests.unit.config;

import ca.bnc.ciam.autotests.config.ContextConfigLoader;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ContextConfigLoader.
 */
@Test(groups = "unit")
public class ContextConfigLoaderTest {

    @BeforeMethod
    public void setUp() {
        // Reset loader and clear system properties
        ContextConfigLoader.reset();
        System.clearProperty("testEnvironment");
        System.clearProperty("bnc.web.gui.lang");
        System.clearProperty("bnc.web.app.url");
        System.clearProperty("bnc.test.hub.use");
    }

    @AfterMethod
    public void tearDown() {
        // Clean up system properties
        ContextConfigLoader.reset();
        System.clearProperty("testEnvironment");
        System.clearProperty("bnc.web.gui.lang");
        System.clearProperty("bnc.web.app.url");
        System.clearProperty("bnc.test.hub.use");
    }

    @Test
    public void testGetInstance_ReturnsSingleton() {
        ContextConfigLoader instance1 = ContextConfigLoader.getInstance();
        ContextConfigLoader instance2 = ContextConfigLoader.getInstance();

        assertThat(instance1).isSameAs(instance2);
    }

    @Test
    public void testIsPipelineMode_WhenTestEnvironmentNotSet() {
        System.clearProperty("testEnvironment");

        boolean result = ContextConfigLoader.getInstance().isPipelineMode();

        assertThat(result).isFalse();
    }

    @Test
    public void testIsPipelineMode_WhenTestEnvironmentSet() {
        System.setProperty("testEnvironment", "staging-ta");

        boolean result = ContextConfigLoader.getInstance().isPipelineMode();

        assertThat(result).isTrue();
    }

    @Test
    public void testIsPipelineMode_WhenTestEnvironmentEmpty() {
        System.setProperty("testEnvironment", "");

        boolean result = ContextConfigLoader.getInstance().isPipelineMode();

        assertThat(result).isFalse();
    }

    @Test
    public void testGetTestEnvironment_ReturnsValue() {
        System.setProperty("testEnvironment", "production");

        String result = ContextConfigLoader.getInstance().getTestEnvironment();

        assertThat(result).isEqualTo("production");
    }

    @Test
    public void testGetTestEnvironment_ReturnsNull() {
        System.clearProperty("testEnvironment");

        String result = ContextConfigLoader.getInstance().getTestEnvironment();

        assertThat(result).isNull();
    }

    @Test
    public void testLoadConfig_WithNullEnvironment() {
        // Should not throw, just log and skip
        ContextConfigLoader.getInstance().loadConfig(null, "chrome-fr");

        // No properties should be set
        assertThat(System.getProperty("bnc.web.gui.lang")).isNull();
    }

    @Test
    public void testLoadConfig_WithEmptyEnvironment() {
        // Should not throw, just log and skip
        ContextConfigLoader.getInstance().loadConfig("", "chrome-fr");

        // No properties should be set
        assertThat(System.getProperty("bnc.web.gui.lang")).isNull();
    }

    @Test
    public void testLoadConfig_WithNullConfigKey() {
        // Should not throw, just log and skip
        ContextConfigLoader.getInstance().loadConfig("staging-ta", null);

        // No properties should be set
        assertThat(System.getProperty("bnc.web.gui.lang")).isNull();
    }

    @Test
    public void testLoadConfig_WithEmptyConfigKey() {
        // Should not throw, just log and skip
        ContextConfigLoader.getInstance().loadConfig("staging-ta", "");

        // No properties should be set
        assertThat(System.getProperty("bnc.web.gui.lang")).isNull();
    }

    @Test
    public void testGetMergedConfig_WithNullEnvironment() {
        Map<String, String> result = ContextConfigLoader.getInstance()
                .getMergedConfig(null, "chrome-fr");

        assertThat(result).isEmpty();
    }

    @Test
    public void testGetMergedConfig_WithNonExistentEnvironment() {
        // context.json doesn't exist in test resources, so this should handle gracefully
        Map<String, String> result = ContextConfigLoader.getInstance()
                .getMergedConfig("non-existent-env", "chrome-fr");

        // Should return empty or partial config (saucelabs only if exists)
        assertThat(result).isNotNull();
    }

    @Test
    public void testReset_CreatesNewInstance() {
        ContextConfigLoader instance1 = ContextConfigLoader.getInstance();
        ContextConfigLoader.reset();
        ContextConfigLoader instance2 = ContextConfigLoader.getInstance();

        assertThat(instance1).isNotSameAs(instance2);
    }
}
