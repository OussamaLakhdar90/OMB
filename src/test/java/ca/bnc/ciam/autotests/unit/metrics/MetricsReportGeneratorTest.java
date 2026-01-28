package ca.bnc.ciam.autotests.unit.metrics;

import ca.bnc.ciam.autotests.metrics.MetricsReportGenerator;
import ca.bnc.ciam.autotests.metrics.TestMetrics;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MetricsReportGenerator.
 */
@Test(groups = "unit")
public class MetricsReportGeneratorTest {

    private MetricsReportGenerator generator;
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("metrics-test");
        generator = new MetricsReportGenerator(tempDir.toString());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        // Clean up temp directory
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }
    }

    @Test
    public void testGenerateReports_AllFormats() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        Map<String, Path> reports = generator.generateReports(metrics);

        assertThat(reports).containsKeys("json", "csv", "html");
        assertThat(reports.get("json")).exists();
        assertThat(reports.get("csv")).exists();
        assertThat(reports.get("html")).exists();
    }

    @Test
    public void testGenerateReport_Json() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        Path report = generator.generateReport(metrics, "json");

        assertThat(report).exists();
        String content = Files.readString(report);
        assertThat(content).contains("\"suiteName\":\"TestSuite\"");
        assertThat(content).contains("\"totalTests\":3");
    }

    @Test
    public void testGenerateReport_Csv() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        Path report = generator.generateReport(metrics, "csv");

        assertThat(report).exists();
        String content = Files.readString(report);
        assertThat(content).contains("Class,Method,Status,Duration");
        assertThat(content).contains("TestClass,testMethod1,PASSED");
    }

    @Test
    public void testGenerateReport_Html() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        Path report = generator.generateReport(metrics, "html");

        assertThat(report).exists();
        String content = Files.readString(report);
        assertThat(content).contains("<!DOCTYPE html>");
        assertThat(content).contains("Test Execution Report");
        assertThat(content).contains("TestSuite");
    }

    @Test
    public void testGenerateReport_UnknownFormat() {
        TestMetrics metrics = createSampleMetrics();

        assertThatThrownBy(() -> generator.generateReport(metrics, "unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown report format");
    }

    @Test
    public void testGenerateReportString_Json() {
        TestMetrics metrics = createSampleMetrics();

        String report = generator.generateReportString(metrics, "json");

        assertThat(report).isNotNull();
        assertThat(report).contains("\"suiteName\":\"TestSuite\"");
    }

    @Test
    public void testGenerateReportString_Csv() {
        TestMetrics metrics = createSampleMetrics();

        String report = generator.generateReportString(metrics, "csv");

        assertThat(report).isNotNull();
        assertThat(report).contains("Class,Method,Status");
    }

    @Test
    public void testGenerateReportString_Html() {
        TestMetrics metrics = createSampleMetrics();

        String report = generator.generateReportString(metrics, "html");

        assertThat(report).isNotNull();
        assertThat(report).contains("<!DOCTYPE html>");
    }

    @Test
    public void testGetAvailableFormats() {
        Set<String> formats = generator.getAvailableFormats();

        assertThat(formats).contains("json", "csv", "html");
    }

    @Test
    public void testGetOutputDirectory() {
        Path outputDir = generator.getOutputDirectory();

        assertThat(outputDir).isEqualTo(tempDir);
    }

    @Test
    public void testLatestLinksCreated() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        generator.generateReports(metrics);

        assertThat(tempDir.resolve("test-report-latest.json")).exists();
        assertThat(tempDir.resolve("test-report-latest.csv")).exists();
        assertThat(tempDir.resolve("test-report-latest.html")).exists();
    }

    @Test
    public void testCleanupOldReports() throws IOException {
        TestMetrics metrics = createSampleMetrics();

        // Generate some reports
        generator.generateReports(metrics);

        // Cleanup reports older than 0 days (all reports)
        int deleted = generator.cleanupOldReports(0);

        // Should have deleted some files
        assertThat(deleted).isGreaterThan(0);
    }

    @Test
    public void testReportWithVisualMetrics() throws IOException {
        TestMetrics metrics = createSampleMetrics();
        metrics.addVisualMetric(TestMetrics.VisualMetric.builder()
                .testName("VisualTest")
                .baselineName("baseline1")
                .matched(true)
                .diffPercentage(0.005)
                .tolerance(0.01)
                .status("SUCCESS")
                .comparisonTimeMs(150)
                .build());

        Path htmlReport = generator.generateReport(metrics, "html");
        String content = Files.readString(htmlReport);

        assertThat(content).contains("Visual Comparison Results");
        assertThat(content).contains("VisualTest");
    }

    @Test
    public void testReportWithApiMetrics() throws IOException {
        TestMetrics metrics = createSampleMetrics();
        metrics.addApiMetric(TestMetrics.ApiMetric.builder()
                .testName("ApiTest")
                .method("GET")
                .endpoint("/api/users")
                .statusCode(200)
                .responseTimeMs(100)
                .success(true)
                .build());

        Path htmlReport = generator.generateReport(metrics, "html");
        String content = Files.readString(htmlReport);

        assertThat(content).contains("API Call Metrics");
        assertThat(content).contains("ApiTest");
        assertThat(content).contains("/api/users");
    }

    /**
     * Helper method to create sample metrics for testing.
     */
    private TestMetrics createSampleMetrics() {
        TestMetrics metrics = TestMetrics.builder()
                .runId("test-run-123")
                .suiteName("TestSuite")
                .startTime(LocalDateTime.now().minusMinutes(5))
                .endTime(LocalDateTime.now())
                .durationMs(300000)
                .environment("test")
                .browser("chrome")
                .executionMode("local")
                .build();

        metrics.addTestResult(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod1")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(1000)
                .build());

        metrics.addTestResult(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod2")
                .status(TestMetrics.TestResult.Status.FAILED)
                .durationMs(2000)
                .errorMessage("Assertion failed")
                .build());

        metrics.addTestResult(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod3")
                .status(TestMetrics.TestResult.Status.SKIPPED)
                .durationMs(0)
                .build());

        return metrics;
    }
}
