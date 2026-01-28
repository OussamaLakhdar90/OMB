package ca.bnc.ciam.autotests.unit.metrics;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TestMetrics model.
 */
@Test(groups = "unit")
public class TestMetricsTest {

    private TestMetrics metrics;

    @BeforeMethod
    public void setUp() {
        metrics = TestMetrics.builder()
                .runId("run-123")
                .suiteName("TestSuite")
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now())
                .durationMs(1000)
                .environment("test")
                .browser("chrome")
                .executionMode("local")
                .build();
    }

    @Test
    public void testAddTestResult_Passed() {
        TestMetrics.TestResult result = TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(500)
                .build();

        metrics.addTestResult(result);

        assertThat(metrics.getTotalTests()).isEqualTo(1);
        assertThat(metrics.getPassedTests()).isEqualTo(1);
        assertThat(metrics.getFailedTests()).isEqualTo(0);
        assertThat(metrics.getSkippedTests()).isEqualTo(0);
        assertThat(metrics.getPassRate()).isEqualTo(100.0);
    }

    @Test
    public void testAddTestResult_Failed() {
        TestMetrics.TestResult result = TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.FAILED)
                .durationMs(500)
                .errorMessage("Test failed")
                .build();

        metrics.addTestResult(result);

        assertThat(metrics.getTotalTests()).isEqualTo(1);
        assertThat(metrics.getPassedTests()).isEqualTo(0);
        assertThat(metrics.getFailedTests()).isEqualTo(1);
        assertThat(metrics.getPassRate()).isEqualTo(0.0);
    }

    @Test
    public void testAddTestResult_Skipped() {
        TestMetrics.TestResult result = TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.SKIPPED)
                .durationMs(0)
                .build();

        metrics.addTestResult(result);

        assertThat(metrics.getTotalTests()).isEqualTo(1);
        assertThat(metrics.getSkippedTests()).isEqualTo(1);
        assertThat(metrics.getPassRate()).isEqualTo(0.0);
    }

    @Test
    public void testCalculatePassRate_MixedResults() {
        // Add 2 passed, 1 failed, 1 skipped
        metrics.addTestResult(createResult(TestMetrics.TestResult.Status.PASSED));
        metrics.addTestResult(createResult(TestMetrics.TestResult.Status.PASSED));
        metrics.addTestResult(createResult(TestMetrics.TestResult.Status.FAILED));
        metrics.addTestResult(createResult(TestMetrics.TestResult.Status.SKIPPED));

        assertThat(metrics.getTotalTests()).isEqualTo(4);
        assertThat(metrics.getPassedTests()).isEqualTo(2);
        assertThat(metrics.getFailedTests()).isEqualTo(1);
        assertThat(metrics.getSkippedTests()).isEqualTo(1);
        assertThat(metrics.getPassRate()).isEqualTo(50.0);
    }

    @Test
    public void testCalculatePassRate_NoTests() {
        metrics.calculatePassRate();
        assertThat(metrics.getPassRate()).isEqualTo(0.0);
    }

    @Test
    public void testAddVisualMetric() {
        TestMetrics.VisualMetric visualMetric = TestMetrics.VisualMetric.builder()
                .testName("VisualTest")
                .baselineName("baseline1")
                .matched(true)
                .diffPercentage(0.005)
                .tolerance(0.01)
                .status("SUCCESS")
                .comparisonTimeMs(150)
                .build();

        metrics.addVisualMetric(visualMetric);

        assertThat(metrics.getVisualMetrics()).hasSize(1);
        assertThat(metrics.getVisualMetrics().get(0).getTestName()).isEqualTo("VisualTest");
    }

    @Test
    public void testAddApiMetric() {
        TestMetrics.ApiMetric apiMetric = TestMetrics.ApiMetric.builder()
                .testName("ApiTest")
                .method("GET")
                .endpoint("/api/users")
                .statusCode(200)
                .responseTimeMs(100)
                .success(true)
                .build();

        metrics.addApiMetric(apiMetric);

        assertThat(metrics.getApiMetrics()).hasSize(1);
        assertThat(metrics.getApiMetrics().get(0).getMethod()).isEqualTo("GET");
    }

    @Test
    public void testAddCustomMetric() {
        metrics.addCustomMetric("customKey", "customValue");
        metrics.addCustomMetric("numberKey", 42);

        assertThat(metrics.getCustomMetrics()).containsEntry("customKey", "customValue");
        assertThat(metrics.getCustomMetrics()).containsEntry("numberKey", 42);
    }

    @Test
    public void testTestResult_WithSteps() {
        TestMetrics.TestResult result = TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(500)
                .build();

        result.addStep(TestMetrics.StepResult.builder()
                .name("Step 1")
                .status("PASSED")
                .durationMs(100)
                .build());

        result.addStep(TestMetrics.StepResult.builder()
                .name("Step 2")
                .status("PASSED")
                .durationMs(200)
                .build());

        assertThat(result.getSteps()).hasSize(2);
    }

    @Test
    public void testTestResult_WithParameters() {
        TestMetrics.TestResult result = TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(500)
                .build();

        result.getParameters().put("param1", "value1");
        result.getParameters().put("param2", "value2");

        assertThat(result.getParameters()).hasSize(2);
        assertThat(result.getParameters()).containsEntry("param1", "value1");
    }

    @Test
    public void testVisualMetric_WithDiffImage() {
        TestMetrics.VisualMetric visualMetric = TestMetrics.VisualMetric.builder()
                .testName("VisualTest")
                .baselineName("baseline1")
                .matched(false)
                .diffPercentage(0.15)
                .tolerance(0.01)
                .status("FAILURE")
                .diffImagePath("target/visual/diff.png")
                .actualImagePath("target/visual/actual.png")
                .comparisonTimeMs(200)
                .build();

        assertThat(visualMetric.isMatched()).isFalse();
        assertThat(visualMetric.getDiffImagePath()).isEqualTo("target/visual/diff.png");
        assertThat(visualMetric.getActualImagePath()).isEqualTo("target/visual/actual.png");
    }

    @Test
    public void testApiMetric_WithSizes() {
        TestMetrics.ApiMetric apiMetric = TestMetrics.ApiMetric.builder()
                .testName("ApiTest")
                .method("POST")
                .endpoint("/api/users")
                .statusCode(201)
                .responseTimeMs(250)
                .requestSizeBytes(1024)
                .responseSizeBytes(2048)
                .success(true)
                .build();

        assertThat(apiMetric.getRequestSizeBytes()).isEqualTo(1024);
        assertThat(apiMetric.getResponseSizeBytes()).isEqualTo(2048);
    }

    @Test
    public void testStepResult_WithScreenshot() {
        TestMetrics.StepResult step = TestMetrics.StepResult.builder()
                .name("Login Step")
                .status("PASSED")
                .durationMs(300)
                .hasScreenshot(true)
                .screenshotPath("target/screenshots/step1.png")
                .build();

        assertThat(step.isHasScreenshot()).isTrue();
        assertThat(step.getScreenshotPath()).isEqualTo("target/screenshots/step1.png");
    }

    @Test
    public void testStepResult_WithError() {
        TestMetrics.StepResult step = TestMetrics.StepResult.builder()
                .name("Verification Step")
                .status("FAILED")
                .durationMs(500)
                .errorMessage("Element not found")
                .build();

        assertThat(step.getErrorMessage()).isEqualTo("Element not found");
    }

    /**
     * Helper method to create a test result.
     */
    private TestMetrics.TestResult createResult(TestMetrics.TestResult.Status status) {
        return TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(status)
                .durationMs(100)
                .build();
    }
}
