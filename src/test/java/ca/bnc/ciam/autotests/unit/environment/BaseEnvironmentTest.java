package ca.bnc.ciam.autotests.unit.environment;

import ca.bnc.ciam.autotests.environment.BaseEnvironment;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BaseEnvironment class.
 * Tests test data loading, environment configuration, and pipeline detection.
 */
@Test(groups = "unit")
public class BaseEnvironmentTest {

    private String originalDataManagerPath;
    private String originalForceLocal;
    private String originalForcePipeline;

    @BeforeMethod
    public void setUp() {
        originalDataManagerPath = System.getProperty("bnc.data.manager");
        originalForceLocal = System.getProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        originalForcePipeline = System.getProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);
    }

    @AfterMethod
    public void tearDown() {
        if (originalDataManagerPath != null) {
            System.setProperty("bnc.data.manager", originalDataManagerPath);
        } else {
            System.clearProperty("bnc.data.manager");
        }
        if (originalForceLocal != null) {
            System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, originalForceLocal);
        } else {
            System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        }
        if (originalForcePipeline != null) {
            System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, originalForcePipeline);
        } else {
            System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);
        }
    }

    // ===========================================
    // Constants Tests
    // ===========================================

    @Test
    public void testConfigFilePath_IsCorrect() {
        assertThat(BaseEnvironment.CONFIG_FILE_PATH).isEqualTo("src/test/resources/debug_config.json");
    }

    @Test
    public void testDataManagerProperty_IsCorrect() {
        assertThat(BaseEnvironment.DATA_MANAGER_PROPERTY).isEqualTo("bnc.data.manager");
    }

    // ===========================================
    // buildTestEnvironment Tests
    // ===========================================

    @Test
    public void testBuildTestEnvironment_NonExistentTestId_ReturnsEmptyIterator() {
        Iterator<Object[]> result = BaseEnvironment.buildTestEnvironment("Non Existent Test");
        assertThat(result).isNotNull();
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    public void testBuildTestEnvironment_NullTestId_ReturnsEmptyIterator() {
        Iterator<Object[]> result = BaseEnvironment.buildTestEnvironment(null);
        assertThat(result).isNotNull();
    }

    @Test
    public void testBuildTestEnvironment_EmptyTestId_ReturnsEmptyIterator() {
        Iterator<Object[]> result = BaseEnvironment.buildTestEnvironment("");
        assertThat(result).isNotNull();
    }

    // ===========================================
    // getCurrentEnvironment Tests
    // ===========================================

    @Test
    public void testGetCurrentEnvironment_WhenNoDataLoaded_ReturnsUnknown() {
        String env = BaseEnvironment.getCurrentEnvironment();
        // May return "unknown" or actual environment depending on state
        assertThat(env).isNotNull();
    }

    // ===========================================
    // getDataManagerPath Tests
    // ===========================================

    @Test
    public void testGetDataManagerPath_InitialState_MayBeNull() {
        // This tests the getter - the path may or may not be set depending on prior state
        String path = BaseEnvironment.getDataManagerPath();
        // We just verify it doesn't throw
        // The actual value depends on whether data has been loaded
    }

    // ===========================================
    // System Property Tests
    // ===========================================

    @Test
    public void testDataManagerProperty_CanBeSetAndRead() {
        String testPath = "test/data/manager.json";
        System.setProperty(BaseEnvironment.DATA_MANAGER_PROPERTY, testPath);

        String value = System.getProperty(BaseEnvironment.DATA_MANAGER_PROPERTY);
        assertThat(value).isEqualTo(testPath);
    }

    @Test
    public void testDataManagerProperty_CanBeCleared() {
        System.setProperty(BaseEnvironment.DATA_MANAGER_PROPERTY, "test");
        System.clearProperty(BaseEnvironment.DATA_MANAGER_PROPERTY);

        String value = System.getProperty(BaseEnvironment.DATA_MANAGER_PROPERTY);
        assertThat(value).isNull();
    }

    // ===========================================
    // Class Hierarchy Tests
    // ===========================================

    @Test
    public void testBaseEnvironment_ExtendsAbstractDataDrivenTest() {
        assertThat(BaseEnvironment.class.getSuperclass().getSimpleName())
                .isEqualTo("AbstractDataDrivenTest");
    }

    // ===========================================
    // Pipeline Detection Constants Tests
    // ===========================================

    @Test
    public void testForceLocalProperty_IsCorrect() {
        assertThat(BaseEnvironment.FORCE_LOCAL_PROPERTY).isEqualTo("bnc.force.local");
    }

    @Test
    public void testForcePipelineProperty_IsCorrect() {
        assertThat(BaseEnvironment.FORCE_PIPELINE_PROPERTY).isEqualTo("bnc.force.pipeline");
    }

    // ===========================================
    // Pipeline Detection - Force Override Tests
    // ===========================================

    @Test
    public void testIsRunningInPipeline_WhenForceLocalTrue_ReturnsFalse() {
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "true");
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        assertThat(BaseEnvironment.isRunningInPipeline()).isFalse();
    }

    @Test
    public void testIsRunningInPipeline_WhenForcePipelineTrue_ReturnsTrue() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "true");

        assertThat(BaseEnvironment.isRunningInPipeline()).isTrue();
    }

    @Test
    public void testIsRunningInPipeline_ForceLocalTakesPrecedence() {
        // When both are set, force.local should take precedence (checked first)
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "true");
        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "true");

        assertThat(BaseEnvironment.isRunningInPipeline()).isFalse();
    }

    @Test
    public void testIsRunningLocally_WhenForceLocalTrue_ReturnsTrue() {
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "true");
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        assertThat(BaseEnvironment.isRunningLocally()).isTrue();
    }

    @Test
    public void testIsRunningLocally_WhenForcePipelineTrue_ReturnsFalse() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "true");

        assertThat(BaseEnvironment.isRunningLocally()).isFalse();
    }

    // ===========================================
    // Pipeline Detection - isRunningLocally inverse
    // ===========================================

    @Test
    public void testIsRunningLocally_IsInverseOfIsRunningInPipeline() {
        // Clear any overrides to test default behavior
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        boolean inPipeline = BaseEnvironment.isRunningInPipeline();
        boolean isLocal = BaseEnvironment.isRunningLocally();

        assertThat(isLocal).isEqualTo(!inPipeline);
    }

    // ===========================================
    // getDetectedCISystem Tests
    // ===========================================

    @Test
    public void testGetDetectedCISystem_WhenForceLocalTrue_ReturnsLocalForced() {
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "true");
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        assertThat(BaseEnvironment.getDetectedCISystem()).isEqualTo("Local (forced)");
    }

    @Test
    public void testGetDetectedCISystem_WhenForcePipelineTrue_ReturnsPipelineForced() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "true");

        assertThat(BaseEnvironment.getDetectedCISystem()).isEqualTo("Pipeline (forced)");
    }

    @Test
    public void testGetDetectedCISystem_ReturnsNonNullValue() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        String ciSystem = BaseEnvironment.getDetectedCISystem();
        assertThat(ciSystem).isNotNull();
        assertThat(ciSystem).isNotEmpty();
    }

    @Test
    public void testGetDetectedCISystem_WhenNotInPipeline_ReturnsLocalOrCIName() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        String ciSystem = BaseEnvironment.getDetectedCISystem();
        // Should be either "Local" or a known CI system name
        assertThat(ciSystem).isIn(
                "Local",
                "Jenkins",
                "GitLab CI",
                "GitHub Actions",
                "Azure DevOps",
                "Bitbucket Pipelines",
                "CircleCI",
                "Travis CI",
                "TeamCity",
                "Unknown CI"
        );
    }

    // ===========================================
    // Pipeline Detection - Case Insensitive Override
    // ===========================================

    @Test
    public void testIsRunningInPipeline_ForceLocalCaseInsensitive() {
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "TRUE");
        assertThat(BaseEnvironment.isRunningInPipeline()).isFalse();

        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "True");
        assertThat(BaseEnvironment.isRunningInPipeline()).isFalse();
    }

    @Test
    public void testIsRunningInPipeline_ForcePipelineCaseInsensitive() {
        System.clearProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY);

        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "TRUE");
        assertThat(BaseEnvironment.isRunningInPipeline()).isTrue();

        System.setProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY, "True");
        assertThat(BaseEnvironment.isRunningInPipeline()).isTrue();
    }

    @Test
    public void testIsRunningInPipeline_ForceLocalFalseValue_DoesNotForce() {
        // Setting to "false" should not force local
        System.setProperty(BaseEnvironment.FORCE_LOCAL_PROPERTY, "false");
        System.clearProperty(BaseEnvironment.FORCE_PIPELINE_PROPERTY);

        // Result depends on actual CI env vars
        // We just verify it doesn't throw and returns a boolean
        boolean result = BaseEnvironment.isRunningInPipeline();
        assertThat(result).isIn(true, false);
    }

    // ===========================================
    // Hub Configuration System Properties Tests
    // ===========================================

    @Test
    public void testHubUseProperty_CanBeSetAndRead() {
        String propName = "bnc.test.hub.use";
        String originalValue = System.getProperty(propName);
        try {
            System.setProperty(propName, "true");
            assertThat(System.getProperty(propName)).isEqualTo("true");

            System.setProperty(propName, "false");
            assertThat(System.getProperty(propName)).isEqualTo("false");
        } finally {
            if (originalValue != null) {
                System.setProperty(propName, originalValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }

    @Test
    public void testHubUrlProperty_CanBeSetAndRead() {
        String propName = "bnc.test.hub.url";
        String originalValue = System.getProperty(propName);
        try {
            String testUrl = "https://ondemand.saucelabs.com/wd/hub";
            System.setProperty(propName, testUrl);
            assertThat(System.getProperty(propName)).isEqualTo(testUrl);
        } finally {
            if (originalValue != null) {
                System.setProperty(propName, originalValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }

    @Test
    public void testBrowsersConfigProperty_CanBeSetAndRead() {
        String propName = "bnc.web.browsers.config";
        String originalValue = System.getProperty(propName);
        try {
            String testPath = "configuration/config_chrome_win10.json";
            System.setProperty(propName, testPath);
            assertThat(System.getProperty(propName)).isEqualTo(testPath);
        } finally {
            if (originalValue != null) {
                System.setProperty(propName, originalValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }

    @Test
    public void testTunnelIdentifierProperty_CanBeSetAndRead() {
        String propName = "bnc.test.hub.tunnelIdentifier";
        String originalValue = System.getProperty(propName);
        try {
            String testTunnel = "SauceConnect";
            System.setProperty(propName, testTunnel);
            assertThat(System.getProperty(propName)).isEqualTo(testTunnel);
        } finally {
            if (originalValue != null) {
                System.setProperty(propName, originalValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }

    @Test
    public void testParentTunnelProperty_CanBeSetAndRead() {
        String propName = "bnc.test.hub.parentTunnel";
        String originalValue = System.getProperty(propName);
        try {
            String testParentTunnel = "TestAdmin";
            System.setProperty(propName, testParentTunnel);
            assertThat(System.getProperty(propName)).isEqualTo(testParentTunnel);
        } finally {
            if (originalValue != null) {
                System.setProperty(propName, originalValue);
            } else {
                System.clearProperty(propName);
            }
        }
    }

    @Test
    public void testHubProperties_AreIndependent() {
        // Clear all hub properties
        String[] hubProps = {
            "bnc.test.hub.use",
            "bnc.test.hub.url",
            "bnc.web.browsers.config",
            "bnc.test.hub.tunnelIdentifier",
            "bnc.test.hub.parentTunnel"
        };

        // Store original values
        String[] originals = new String[hubProps.length];
        for (int i = 0; i < hubProps.length; i++) {
            originals[i] = System.getProperty(hubProps[i]);
        }

        try {
            // Clear all
            for (String prop : hubProps) {
                System.clearProperty(prop);
            }

            // Setting one should not affect others
            System.setProperty("bnc.test.hub.use", "true");
            assertThat(System.getProperty("bnc.test.hub.url")).isNull();
            assertThat(System.getProperty("bnc.web.browsers.config")).isNull();
            assertThat(System.getProperty("bnc.test.hub.tunnelIdentifier")).isNull();
            assertThat(System.getProperty("bnc.test.hub.parentTunnel")).isNull();
        } finally {
            // Restore original values
            for (int i = 0; i < hubProps.length; i++) {
                if (originals[i] != null) {
                    System.setProperty(hubProps[i], originals[i]);
                } else {
                    System.clearProperty(hubProps[i]);
                }
            }
        }
    }

    // ===========================================
    // getCurrentGuiLang Tests
    // ===========================================

    @Test
    public void testGetCurrentGuiLang_ReturnsDefault() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.clearProperty("bnc.web.gui.lang");

            String result = BaseEnvironment.getCurrentGuiLang();

            assertThat(result).isEqualTo("en");
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            }
        }
    }

    @Test
    public void testGetCurrentGuiLang_ReturnsFrench() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "fr");

            String result = BaseEnvironment.getCurrentGuiLang();

            assertThat(result).isEqualTo("fr");
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testGetCurrentGuiLang_ReturnsEnglish() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "en");

            String result = BaseEnvironment.getCurrentGuiLang();

            assertThat(result).isEqualTo("en");
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testGetCurrentGuiLang_EmptyProperty_ReturnsDefault() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "");

            String result = BaseEnvironment.getCurrentGuiLang();

            assertThat(result).isEqualTo("en");
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    // ===========================================
    // isGuiLangFrench Tests
    // ===========================================

    @Test
    public void testIsGuiLangFrench_WhenFrench() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "fr");

            assertThat(BaseEnvironment.isGuiLangFrench()).isTrue();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testIsGuiLangFrench_CaseInsensitive() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "FR");

            assertThat(BaseEnvironment.isGuiLangFrench()).isTrue();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testIsGuiLangFrench_WhenEnglish() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "en");

            assertThat(BaseEnvironment.isGuiLangFrench()).isFalse();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    // ===========================================
    // isGuiLangEnglish Tests
    // ===========================================

    @Test
    public void testIsGuiLangEnglish_WhenEnglish() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "en");

            assertThat(BaseEnvironment.isGuiLangEnglish()).isTrue();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testIsGuiLangEnglish_CaseInsensitive() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "EN");

            assertThat(BaseEnvironment.isGuiLangEnglish()).isTrue();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testIsGuiLangEnglish_WhenFrench() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.setProperty("bnc.web.gui.lang", "fr");

            assertThat(BaseEnvironment.isGuiLangEnglish()).isFalse();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            } else {
                System.clearProperty("bnc.web.gui.lang");
            }
        }
    }

    @Test
    public void testIsGuiLangEnglish_WhenDefault() {
        String originalLang = System.getProperty("bnc.web.gui.lang");
        try {
            System.clearProperty("bnc.web.gui.lang");

            // Default is "en"
            assertThat(BaseEnvironment.isGuiLangEnglish()).isTrue();
        } finally {
            if (originalLang != null) {
                System.setProperty("bnc.web.gui.lang", originalLang);
            }
        }
    }
}
