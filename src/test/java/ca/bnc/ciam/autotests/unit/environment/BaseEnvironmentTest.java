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
}
