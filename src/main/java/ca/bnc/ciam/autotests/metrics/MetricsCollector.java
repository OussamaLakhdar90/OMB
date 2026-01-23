package ca.bnc.ciam.autotests.metrics;

import ca.bnc.ciam.autotests.utils.Utils;
import ca.bnc.ciam.autotests.web.WebDriverFactory;
import ca.bnc.ciam.autotests.web.config.WebConfig;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects test execution metrics throughout test runs.
 */
@Slf4j
public class MetricsCollector {

    private static final MetricsCollector INSTANCE = new MetricsCollector();

    private TestMetrics currentMetrics;
    private final Map<String, LocalDateTime> testStartTimes = new ConcurrentHashMap<>();
    private final Map<String, TestMetrics.TestResult.TestResultBuilder> testBuilders = new ConcurrentHashMap<>();

    private MetricsCollector() {
    }

    /**
     * Get singleton instance.
     */
    public static MetricsCollector getInstance() {
        return INSTANCE;
    }

    /**
     * Initialize metrics collection for a test suite.
     */
    public void startSuite(ITestContext context) {
        String runId = Utils.generateRunId();
        log.info("Starting metrics collection for suite: {} (runId: {})", context.getName(), runId);

        // Get browser/environment info
        WebConfig config = WebDriverFactory.getConfig();
        String browser = config != null ? config.getBrowserType().getName() : "unknown";
        String executionMode = config != null ? config.getExecutionMode().getName() : "local";

        currentMetrics = TestMetrics.builder()
                .runId(runId)
                .suiteName(context.getName())
                .startTime(dateToLocalDateTime(context.getStartDate()))
                .environment(System.getProperty("env", "default"))
                .browser(browser)
                .executionMode(executionMode)
                .build();
    }

    /**
     * Finalize metrics collection for a test suite.
     */
    public TestMetrics endSuite(ITestContext context) {
        if (currentMetrics == null) {
            log.warn("No active metrics collection. Call startSuite first.");
            return null;
        }

        currentMetrics.setEndTime(dateToLocalDateTime(context.getEndDate()));
        currentMetrics.setDurationMs(
                currentMetrics.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
                        currentMetrics.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );

        log.info("Metrics collection complete for suite: {}. Total: {}, Passed: {}, Failed: {}, Skipped: {}",
                currentMetrics.getSuiteName(),
                currentMetrics.getTotalTests(),
                currentMetrics.getPassedTests(),
                currentMetrics.getFailedTests(),
                currentMetrics.getSkippedTests());

        return currentMetrics;
    }

    /**
     * Record test method start.
     */
    public void startTest(ITestResult result) {
        String testKey = getTestKey(result);
        LocalDateTime startTime = LocalDateTime.now();
        testStartTimes.put(testKey, startTime);

        TestMetrics.TestResult.TestResultBuilder builder = TestMetrics.TestResult.builder()
                .className(result.getTestClass().getName())
                .methodName(result.getMethod().getMethodName())
                .startTime(startTime);

        // Add test parameters if any
        Object[] parameters = result.getParameters();
        if (parameters != null && parameters.length > 0) {
            for (int i = 0; i < parameters.length; i++) {
                builder.parameters(Map.of("param" + i, String.valueOf(parameters[i])));
            }
        }

        testBuilders.put(testKey, builder);
    }

    /**
     * Record test method end.
     */
    public void endTest(ITestResult result) {
        if (currentMetrics == null) {
            return;
        }

        String testKey = getTestKey(result);
        LocalDateTime startTime = testStartTimes.remove(testKey);
        TestMetrics.TestResult.TestResultBuilder builder = testBuilders.remove(testKey);

        if (builder == null) {
            log.warn("No start record found for test: {}", testKey);
            builder = TestMetrics.TestResult.builder()
                    .className(result.getTestClass().getName())
                    .methodName(result.getMethod().getMethodName());
        }

        LocalDateTime endTime = LocalDateTime.now();
        long duration = startTime != null ?
                java.time.Duration.between(startTime, endTime).toMillis() :
                result.getEndMillis() - result.getStartMillis();

        TestMetrics.TestResult.Status status = switch (result.getStatus()) {
            case ITestResult.SUCCESS -> TestMetrics.TestResult.Status.PASSED;
            case ITestResult.FAILURE -> TestMetrics.TestResult.Status.FAILED;
            default -> TestMetrics.TestResult.Status.SKIPPED;
        };

        builder.endTime(endTime)
                .durationMs(duration)
                .status(status);

        // Add error info if failed
        if (result.getThrowable() != null) {
            builder.errorMessage(result.getThrowable().getMessage());
            builder.stackTrace(getStackTraceString(result.getThrowable()));
        }

        currentMetrics.addTestResult(builder.build());
    }

    /**
     * Record a step within a test.
     */
    public void recordStep(String testKey, String stepName, String status, long durationMs, String errorMessage) {
        TestMetrics.TestResult.TestResultBuilder builder = testBuilders.get(testKey);
        if (builder == null) {
            log.warn("No active test found for step recording: {}", testKey);
            return;
        }

        TestMetrics.StepResult step = TestMetrics.StepResult.builder()
                .name(stepName)
                .status(status)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();

        builder.build().addStep(step);
    }

    /**
     * Record visual comparison metric.
     */
    public void recordVisualMetric(String testName, String baselineName, boolean matched,
                                    double diffPercentage, double tolerance, String status,
                                    String diffImagePath, long comparisonTimeMs) {
        if (currentMetrics == null) {
            return;
        }

        TestMetrics.VisualMetric metric = TestMetrics.VisualMetric.builder()
                .testName(testName)
                .baselineName(baselineName)
                .matched(matched)
                .diffPercentage(diffPercentage)
                .tolerance(tolerance)
                .status(status)
                .diffImagePath(diffImagePath)
                .comparisonTimeMs(comparisonTimeMs)
                .build();

        currentMetrics.addVisualMetric(metric);
    }

    /**
     * Record API call metric.
     */
    public void recordApiMetric(String testName, String method, String endpoint,
                                 int statusCode, long responseTimeMs,
                                 long requestSizeBytes, long responseSizeBytes) {
        if (currentMetrics == null) {
            return;
        }

        TestMetrics.ApiMetric metric = TestMetrics.ApiMetric.builder()
                .testName(testName)
                .method(method)
                .endpoint(endpoint)
                .statusCode(statusCode)
                .responseTimeMs(responseTimeMs)
                .requestSizeBytes(requestSizeBytes)
                .responseSizeBytes(responseSizeBytes)
                .success(statusCode >= 200 && statusCode < 300)
                .build();

        currentMetrics.addApiMetric(metric);
    }

    /**
     * Add custom metric.
     */
    public void addCustomMetric(String key, Object value) {
        if (currentMetrics != null) {
            currentMetrics.addCustomMetric(key, value);
        }
    }

    /**
     * Get current metrics.
     */
    public TestMetrics getCurrentMetrics() {
        return currentMetrics;
    }

    /**
     * Get unique test key.
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "#" + result.getMethod().getMethodName();
    }

    /**
     * Convert Date to LocalDateTime.
     */
    private LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Get stack trace as string.
     */
    private String getStackTraceString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Reset collector state.
     */
    public void reset() {
        currentMetrics = null;
        testStartTimes.clear();
        testBuilders.clear();
    }
}
