package ca.bnc.ciam.autotests.unit.xray;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import ca.bnc.ciam.autotests.xray.JiraWikiMarkupFormatter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JiraWikiMarkupFormatter.
 * Tests formatting of TestMetrics as Jira Wiki Markup.
 */
@Test(groups = "unit")
public class JiraWikiMarkupFormatterTest {

    private TestMetrics metrics;

    @BeforeMethod
    public void setUp() {
        // Create sample metrics for testing
        metrics = TestMetrics.builder()
                .runId("test-run-123")
                .suiteName("Login Tests")
                .startTime(LocalDateTime.of(2026, 1, 21, 14, 30, 0))
                .endTime(LocalDateTime.of(2026, 1, 21, 14, 35, 32))
                .durationMs(332000) // 5m 32s
                .environment("staging-ta")
                .browser("Chrome 120")
                .executionMode("saucelabs")
                .totalTests(50)
                .passedTests(45)
                .failedTests(3)
                .skippedTests(2)
                .passRate(90.0)
                .testResults(new ArrayList<>())
                .apiMetrics(new ArrayList<>())
                .visualMetrics(new ArrayList<>())
                .customMetrics(new HashMap<>())
                .build();
    }

    // ===========================================
    // format() Tests
    // ===========================================

    @Test
    public void testFormat_BasicMetrics_ContainsHeader() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h2. Test Execution Metrics");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsRunId() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("test-run-123");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsSuiteName() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("Login Tests");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsEnvironment() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("staging-ta");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsBrowser() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("Chrome 120");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsExecutionMode() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("saucelabs");
    }

    @Test
    public void testFormat_BasicMetrics_ContainsTestResultsSummary() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h3. Test Results");
        assertThat(result).contains("50"); // total
        assertThat(result).contains("45"); // passed
        assertThat(result).contains("3");  // failed
        assertThat(result).contains("2");  // skipped
    }

    @Test
    public void testFormat_HighPassRate_UsesGreenColor() {
        metrics.setPassRate(95.0);
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("{color:green}");
        assertThat(result).contains("95.0%");
    }

    @Test
    public void testFormat_MediumPassRate_UsesOrangeColor() {
        metrics.setPassRate(75.0);
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("{color:orange}");
        assertThat(result).contains("75.0%");
    }

    @Test
    public void testFormat_LowPassRate_UsesRedColor() {
        metrics.setPassRate(50.0);
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("{color:red}");
        assertThat(result).contains("50.0%");
    }

    @Test
    public void testFormat_ContainsTableMarkup() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("||"); // Table header markers
        assertThat(result).contains("|");  // Table cell markers
    }

    @Test
    public void testFormat_ContainsFooter() {
        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("----");
        assertThat(result).contains("Generated automatically");
    }

    // ===========================================
    // format() with TestResults Tests
    // ===========================================

    @Test
    public void testFormat_WithTestResults_ContainsTestDetailsSection() {
        List<TestMetrics.TestResult> testResults = new ArrayList<>();
        testResults.add(TestMetrics.TestResult.builder()
                .className("com.example.LoginTest")
                .methodName("testLogin")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(1500)
                .build());

        metrics.setTestResults(testResults);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h3. Test Details");
        assertThat(result).contains("LoginTest");
        assertThat(result).contains("testLogin");
        assertThat(result).contains("PASSED");
    }

    @Test
    public void testFormat_PassedTest_UsesGreenColorAndCheckmark() {
        List<TestMetrics.TestResult> testResults = new ArrayList<>();
        testResults.add(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.PASSED)
                .durationMs(100)
                .build());

        metrics.setTestResults(testResults);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("(/)"); // checkmark
        assertThat(result).contains("{color:green}");
    }

    @Test
    public void testFormat_FailedTest_UsesRedColorAndXMark() {
        List<TestMetrics.TestResult> testResults = new ArrayList<>();
        testResults.add(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.FAILED)
                .durationMs(100)
                .errorMessage("Element not found")
                .build());

        metrics.setTestResults(testResults);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("(x)"); // X mark
        assertThat(result).contains("{color:red}");
        assertThat(result).contains("Element not found");
    }

    @Test
    public void testFormat_SkippedTest_UsesOrangeColorAndExclamation() {
        List<TestMetrics.TestResult> testResults = new ArrayList<>();
        testResults.add(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.SKIPPED)
                .durationMs(0)
                .build());

        metrics.setTestResults(testResults);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("(!)"); // exclamation
        assertThat(result).contains("{color:orange}");
    }

    // ===========================================
    // format() with API Metrics Tests
    // ===========================================

    @Test
    public void testFormat_WithApiMetrics_ContainsApiSection() {
        List<TestMetrics.ApiMetric> apiMetrics = new ArrayList<>();
        apiMetrics.add(TestMetrics.ApiMetric.builder()
                .testName("testLogin")
                .method("POST")
                .endpoint("/api/auth/login")
                .statusCode(200)
                .responseTimeMs(156)
                .success(true)
                .build());

        metrics.setApiMetrics(apiMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h3. API Metrics");
        assertThat(result).contains("POST");
        assertThat(result).contains("/api/auth/login");
        assertThat(result).contains("200");
        assertThat(result).contains("156ms");
    }

    @Test
    public void testFormat_SuccessfulApiCall_UsesGreenColor() {
        List<TestMetrics.ApiMetric> apiMetrics = new ArrayList<>();
        apiMetrics.add(TestMetrics.ApiMetric.builder()
                .testName("test")
                .method("GET")
                .endpoint("/api")
                .statusCode(200)
                .success(true)
                .build());

        metrics.setApiMetrics(apiMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("{color:green}200{color}");
    }

    @Test
    public void testFormat_FailedApiCall_UsesRedColor() {
        List<TestMetrics.ApiMetric> apiMetrics = new ArrayList<>();
        apiMetrics.add(TestMetrics.ApiMetric.builder()
                .testName("test")
                .method("GET")
                .endpoint("/api")
                .statusCode(500)
                .success(false)
                .build());

        metrics.setApiMetrics(apiMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("{color:red}500{color}");
    }

    // ===========================================
    // format() with Visual Metrics Tests
    // ===========================================

    @Test
    public void testFormat_WithVisualMetrics_ContainsVisualSection() {
        List<TestMetrics.VisualMetric> visualMetrics = new ArrayList<>();
        visualMetrics.add(TestMetrics.VisualMetric.builder()
                .testName("testLoginPage")
                .baselineName("login_baseline")
                .matched(true)
                .diffPercentage(0.0002)
                .status("SUCCESS")
                .build());

        metrics.setVisualMetrics(visualMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h3. Visual Comparison Results");
        assertThat(result).contains("testLoginPage");
        assertThat(result).contains("login_baseline");
        assertThat(result).contains("SUCCESS");
    }

    @Test
    public void testFormat_MatchedVisual_UsesGreenAndCheckmark() {
        List<TestMetrics.VisualMetric> visualMetrics = new ArrayList<>();
        visualMetrics.add(TestMetrics.VisualMetric.builder()
                .testName("test")
                .baselineName("baseline")
                .matched(true)
                .diffPercentage(0.001)
                .status("SUCCESS")
                .build());

        metrics.setVisualMetrics(visualMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("(/)");
        assertThat(result).contains("{color:green}");
        assertThat(result).contains("Yes");
    }

    @Test
    public void testFormat_MismatchedVisual_UsesRedAndXMark() {
        List<TestMetrics.VisualMetric> visualMetrics = new ArrayList<>();
        visualMetrics.add(TestMetrics.VisualMetric.builder()
                .testName("test")
                .baselineName("baseline")
                .matched(false)
                .diffPercentage(0.05)
                .status("FAILURE")
                .build());

        metrics.setVisualMetrics(visualMetrics);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("(x)");
        assertThat(result).contains("{color:red}");
        assertThat(result).contains("No");
    }

    // ===========================================
    // format() with Custom Metrics Tests
    // ===========================================

    @Test
    public void testFormat_WithCustomMetrics_ContainsCustomSection() {
        metrics.getCustomMetrics().put("loginAttempts", 3);
        metrics.getCustomMetrics().put("sessionDuration", "5m");

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("h3. Custom Metrics");
        assertThat(result).contains("loginAttempts");
        assertThat(result).contains("3");
        assertThat(result).contains("sessionDuration");
        assertThat(result).contains("5m");
    }

    // ===========================================
    // formatCompact() Tests
    // ===========================================

    @Test
    public void testFormatCompact_ContainsRunId() {
        String result = JiraWikiMarkupFormatter.formatCompact(metrics);

        assertThat(result).contains("test-run-123");
    }

    @Test
    public void testFormatCompact_ContainsSummaryTable() {
        String result = JiraWikiMarkupFormatter.formatCompact(metrics);

        assertThat(result).contains("||Metric||Value||");
        assertThat(result).contains("|Pass Rate|");
        assertThat(result).contains("|Total|");
        assertThat(result).contains("|Passed|");
        assertThat(result).contains("|Failed|");
        assertThat(result).contains("|Skipped|");
    }

    @Test
    public void testFormatCompact_IsShorterThanFull() {
        String fullFormat = JiraWikiMarkupFormatter.format(metrics);
        String compactFormat = JiraWikiMarkupFormatter.formatCompact(metrics);

        assertThat(compactFormat.length()).isLessThan(fullFormat.length());
    }

    // ===========================================
    // Edge Cases Tests
    // ===========================================

    @Test
    public void testFormat_NullRunId_HandlesGracefully() {
        metrics.setRunId(null);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).isNotNull();
        assertThat(result).contains("|Run ID|");
    }

    @Test
    public void testFormat_NullEnvironment_HandlesGracefully() {
        metrics.setEnvironment(null);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).isNotNull();
        assertThat(result).contains("|Environment|");
    }

    @Test
    public void testFormat_EmptyTestResults_OmitsTestDetailsSection() {
        metrics.setTestResults(new ArrayList<>());

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).doesNotContain("h3. Test Details");
    }

    @Test
    public void testFormat_EmptyApiMetrics_OmitsApiSection() {
        metrics.setApiMetrics(new ArrayList<>());

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).doesNotContain("h3. API Metrics");
    }

    @Test
    public void testFormat_EmptyVisualMetrics_OmitsVisualSection() {
        metrics.setVisualMetrics(new ArrayList<>());

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).doesNotContain("h3. Visual Comparison Results");
    }

    @Test
    public void testFormat_EmptyCustomMetrics_OmitsCustomSection() {
        metrics.setCustomMetrics(new HashMap<>());

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).doesNotContain("h3. Custom Metrics");
    }

    @Test
    public void testFormat_ErrorMessageWithSpecialChars_EscapesWikiMarkup() {
        List<TestMetrics.TestResult> testResults = new ArrayList<>();
        testResults.add(TestMetrics.TestResult.builder()
                .className("TestClass")
                .methodName("testMethod")
                .status(TestMetrics.TestResult.Status.FAILED)
                .durationMs(100)
                .errorMessage("Error with {special} [chars] and |pipes|")
                .build());

        metrics.setTestResults(testResults);

        String result = JiraWikiMarkupFormatter.format(metrics);

        // Should escape special characters
        assertThat(result).contains("\\{");
        assertThat(result).contains("\\}");
        assertThat(result).contains("\\[");
        assertThat(result).contains("\\]");
    }

    // ===========================================
    // Duration Formatting Tests
    // ===========================================

    @Test
    public void testFormat_MillisecondDuration_FormatsAsMs() {
        metrics.setDurationMs(500);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("500ms");
    }

    @Test
    public void testFormat_SecondDuration_FormatsAsSeconds() {
        metrics.setDurationMs(5000);

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("5.0s");
    }

    @Test
    public void testFormat_MinuteDuration_FormatsAsMinutesAndSeconds() {
        metrics.setDurationMs(332000); // 5m 32s

        String result = JiraWikiMarkupFormatter.format(metrics);

        assertThat(result).contains("5m 32s");
    }
}
