package ca.bnc.ciam.autotests.metrics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Model for test execution metrics.
 */
@Data
@Builder
public class TestMetrics {

    /**
     * Unique run identifier.
     */
    private String runId;

    /**
     * Test suite name.
     */
    private String suiteName;

    /**
     * Execution start time.
     */
    private LocalDateTime startTime;

    /**
     * Execution end time.
     */
    private LocalDateTime endTime;

    /**
     * Total execution duration in milliseconds.
     */
    private long durationMs;

    /**
     * Environment name.
     */
    private String environment;

    /**
     * Browser type.
     */
    private String browser;

    /**
     * Execution mode (local/saucelabs).
     */
    private String executionMode;

    /**
     * Total number of tests.
     */
    private int totalTests;

    /**
     * Number of passed tests.
     */
    private int passedTests;

    /**
     * Number of failed tests.
     */
    private int failedTests;

    /**
     * Number of skipped tests.
     */
    private int skippedTests;

    /**
     * Pass rate percentage.
     */
    private double passRate;

    /**
     * Individual test results.
     */
    @Builder.Default
    private List<TestResult> testResults = new ArrayList<>();

    /**
     * Visual comparison results.
     */
    @Builder.Default
    private List<VisualMetric> visualMetrics = new ArrayList<>();

    /**
     * API call metrics.
     */
    @Builder.Default
    private List<ApiMetric> apiMetrics = new ArrayList<>();

    /**
     * Custom metrics.
     */
    @Builder.Default
    private Map<String, Object> customMetrics = new HashMap<>();

    /**
     * Calculate pass rate.
     */
    public void calculatePassRate() {
        if (totalTests > 0) {
            this.passRate = (passedTests * 100.0) / totalTests;
        } else {
            this.passRate = 0.0;
        }
    }

    /**
     * Add a test result.
     */
    public void addTestResult(TestResult result) {
        testResults.add(result);
        totalTests++;
        switch (result.getStatus()) {
            case PASSED -> passedTests++;
            case FAILED -> failedTests++;
            case SKIPPED -> skippedTests++;
        }
        calculatePassRate();
    }

    /**
     * Add visual metric.
     */
    public void addVisualMetric(VisualMetric metric) {
        visualMetrics.add(metric);
    }

    /**
     * Add API metric.
     */
    public void addApiMetric(ApiMetric metric) {
        apiMetrics.add(metric);
    }

    /**
     * Add custom metric.
     */
    public void addCustomMetric(String key, Object value) {
        customMetrics.put(key, value);
    }

    /**
     * Individual test result.
     */
    @Data
    @Builder
    public static class TestResult {
        private String className;
        private String methodName;
        private Status status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private long durationMs;
        private String errorMessage;
        private String stackTrace;
        @Builder.Default
        private List<StepResult> steps = new ArrayList<>();
        @Builder.Default
        private Map<String, String> parameters = new HashMap<>();

        public enum Status {
            PASSED, FAILED, SKIPPED
        }

        public void addStep(StepResult step) {
            steps.add(step);
        }
    }

    /**
     * Individual step result.
     */
    @Data
    @Builder
    public static class StepResult {
        private String name;
        private String status;
        private long durationMs;
        private String errorMessage;
        private boolean hasScreenshot;
        private String screenshotPath;
    }

    /**
     * Visual comparison metric.
     */
    @Data
    @Builder
    public static class VisualMetric {
        private String testName;
        private String baselineName;
        private boolean matched;
        private double diffPercentage;
        private double tolerance;
        private String status; // SUCCESS, FAILURE, WARNING, BASELINE_CREATED
        private String diffImagePath;
        private String actualImagePath;
        private long comparisonTimeMs;
    }

    /**
     * API call metric.
     */
    @Data
    @Builder
    public static class ApiMetric {
        private String testName;
        private String method;
        private String endpoint;
        private int statusCode;
        private long responseTimeMs;
        private long requestSizeBytes;
        private long responseSizeBytes;
        private boolean success;
    }
}
