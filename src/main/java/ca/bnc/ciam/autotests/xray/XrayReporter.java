package ca.bnc.ciam.autotests.xray;

import ca.bnc.ciam.autotests.annotation.Xray;
import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporter that collects test results and reports to Xray.
 * Aggregates results at class level (one test per class with steps).
 */
@Slf4j
public class XrayReporter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final XrayClient client;
    private final XrayConfig config;

    /**
     * Test class results accumulator.
     * Key: Test class name, Value: List of step results
     */
    private final Map<String, TestClassResult> classResults = new HashMap<>();

    /**
     * Create reporter with configuration.
     */
    public XrayReporter(XrayConfig config) {
        this.config = config;
        this.client = new XrayClient(config);
    }

    /**
     * Create reporter from environment.
     */
    public static XrayReporter fromEnvironment() {
        return new XrayReporter(XrayConfig.fromEnvironment());
    }

    /**
     * Record test method result as a step.
     */
    public void recordStep(ITestResult result) {
        if (!config.isEnabled()) {
            return;
        }

        Class<?> testClass = result.getTestClass().getRealClass();
        String className = testClass.getName();
        String methodName = result.getMethod().getMethodName();

        // Get or create class result
        TestClassResult classResult = classResults.computeIfAbsent(className, k -> {
            TestClassResult cr = new TestClassResult();
            cr.testClass = testClass;
            cr.className = className;
            cr.startTime = LocalDateTime.now();

            // Get Xray annotation from class
            Xray xray = testClass.getAnnotation(Xray.class);
            if (xray != null) {
                cr.testKey = xray.test();
                cr.requirementKey = xray.requirement();
                cr.summary = xray.summary();
            }

            return cr;
        });

        // Create step result
        XrayTestExecution.StepResult stepResult;
        if (result.isSuccess()) {
            stepResult = XrayTestExecution.StepResult.passed(methodName);
        } else if (result.getStatus() == ITestResult.SKIP) {
            stepResult = XrayTestExecution.StepResult.builder()
                    .status("TODO")
                    .comment("Skipped: " + getFailureMessage(result))
                    .build();
        } else {
            stepResult = XrayTestExecution.StepResult.failed(getFailureMessage(result));
            classResult.hasFailed = true;
        }

        classResult.steps.add(stepResult);
        classResult.finishTime = LocalDateTime.now();
    }

    /**
     * Record screenshot evidence for current test.
     */
    public void recordEvidence(ITestResult result, String filename, byte[] data) {
        if (!config.isEnabled()) {
            return;
        }

        String className = result.getTestClass().getRealClass().getName();
        TestClassResult classResult = classResults.get(className);

        if (classResult != null) {
            classResult.evidences.add(XrayTestExecution.Evidence.screenshot(filename, data));
        }
    }

    /**
     * Report all collected results to Xray.
     */
    public void reportToXray(ITestContext context) {
        if (!config.isEnabled() || !config.isValid()) {
            log.info("Xray reporting is disabled or not configured");
            return;
        }

        if (classResults.isEmpty()) {
            log.info("No test results to report");
            return;
        }

        log.info("Reporting {} test class results to Xray", classResults.size());

        try {
            // Authenticate
            client.authenticate();

            if (!client.isAuthenticated()) {
                log.error("Failed to authenticate with Xray. Results not reported.");
                return;
            }

            // Build test execution
            XrayTestExecution execution = buildExecution(context);

            // Import to Xray
            String executionKey = client.importExecution(execution);

            if (executionKey != null) {
                log.info("Successfully reported to Xray. Test Execution: {}", executionKey);
            } else {
                log.error("Failed to report to Xray");
            }

        } catch (Exception e) {
            log.error("Error reporting to Xray", e);
        }
    }

    /**
     * Build Xray test execution from collected results.
     */
    private XrayTestExecution buildExecution(ITestContext context) {
        XrayTestExecution.Info info = XrayTestExecution.Info.builder()
                .project(config.getProjectKey())
                .summary("Test Execution - " + context.getName() + " - " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .startDate(context.getStartDate().toString())
                .finishDate(context.getEndDate().toString())
                .testPlanKey(config.getTestPlanKey())
                .version(config.getVersion())
                .revision(config.getRevision())
                .build();

        if (config.getEnvironment() != null) {
            info.getTestEnvironments().add(config.getEnvironment());
        }

        XrayTestExecution.XrayTestExecutionBuilder executionBuilder = XrayTestExecution.builder()
                .info(info);

        // Use existing execution key if provided
        if (config.getTestExecutionKey() != null && !config.getTestExecutionKey().isEmpty()) {
            executionBuilder.testExecutionKey(config.getTestExecutionKey());
        }

        XrayTestExecution execution = executionBuilder.build();

        // Add test results
        for (TestClassResult classResult : classResults.values()) {
            if (classResult.testKey == null || classResult.testKey.isEmpty()) {
                log.warn("Skipping class {} - no @Xray annotation with test key", classResult.className);
                continue;
            }

            XrayTestExecution.TestResult testResult = XrayTestExecution.TestResult.builder()
                    .testKey(classResult.testKey)
                    .status(classResult.hasFailed ? "FAILED" : "PASSED")
                    .start(classResult.startTime.format(DATE_FORMATTER))
                    .finish(classResult.finishTime.format(DATE_FORMATTER))
                    .comment(buildComment(classResult))
                    .steps(classResult.steps)
                    .evidences(classResult.evidences)
                    .build();

            execution.addTest(testResult);
        }

        return execution;
    }

    /**
     * Build comment from class result.
     */
    private String buildComment(TestClassResult classResult) {
        StringBuilder comment = new StringBuilder();
        comment.append("Test Class: ").append(classResult.className).append("\n");
        comment.append("Steps Executed: ").append(classResult.steps.size()).append("\n");

        long passed = classResult.steps.stream()
                .filter(s -> "PASSED".equals(s.getStatus()))
                .count();
        long failed = classResult.steps.stream()
                .filter(s -> "FAILED".equals(s.getStatus()))
                .count();

        comment.append("Passed: ").append(passed).append(", Failed: ").append(failed);

        return comment.toString();
    }

    /**
     * Get failure message from test result.
     */
    private String getFailureMessage(ITestResult result) {
        if (result.getThrowable() != null) {
            String message = result.getThrowable().getMessage();
            return message != null ? message : result.getThrowable().getClass().getName();
        }
        return "Unknown failure";
    }

    /**
     * Clear collected results.
     */
    public void clear() {
        classResults.clear();
    }

    /**
     * Get client for advanced operations.
     */
    public XrayClient getClient() {
        return client;
    }

    /**
     * Internal class to track results per test class.
     */
    private static class TestClassResult {
        Class<?> testClass;
        String className;
        String testKey;
        String requirementKey;
        String summary;
        LocalDateTime startTime;
        LocalDateTime finishTime;
        boolean hasFailed = false;
        List<XrayTestExecution.StepResult> steps = new ArrayList<>();
        List<XrayTestExecution.Evidence> evidences = new ArrayList<>();
    }
}
