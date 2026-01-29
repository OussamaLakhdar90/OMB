package ca.bnc.ciam.autotests.listener;

import ca.bnc.ciam.autotests.annotation.DependentStep;
import ca.bnc.ciam.autotests.annotation.SkipVisualCheck;
import ca.bnc.ciam.autotests.annotation.VisualCheckpoint;
import ca.bnc.ciam.autotests.annotation.Xray;
import ca.bnc.ciam.autotests.config.ContextConfigLoader;
import ca.bnc.ciam.autotests.metrics.MetricsCollector;
import ca.bnc.ciam.autotests.metrics.MetricsReportGenerator;
import ca.bnc.ciam.autotests.metrics.TestMetrics;
import lombok.extern.slf4j.Slf4j;
import org.testng.IAnnotationTransformer;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener that provides:
 * - Lexicographic ordering of test methods (t001, t002, etc.)
 * - Automatic dependency checking via @DependentStep annotation
 * - Visual checkpoint handling
 * - Test lifecycle logging
 * - Automatic test report generation (HTML, JSON, CSV)
 * - Automatic retry of failed tests (configurable via bnc.test.retry.enabled)
 * - Pipeline context configuration loading (when testEnvironment is set)
 *
 * Dependency checking works automatically:
 * - Tests with @DependentStep are skipped if ANY previous test in the class failed
 * - Tests with @DependentStep(value="methodName") are skipped if that specific method failed
 * - No manual calls needed in test code
 *
 * Reports are generated automatically at suite completion:
 * - Output: target/metrics/test-report-latest.html (and .json, .csv)
 * - Disable: Set system property bnc.metrics.enabled=false
 *
 * Retry configuration:
 * - bnc.test.retry.enabled=false to disable retry (default: true)
 * - bnc.test.retry.count=N to set max retry attempts (default: 1)
 *
 * Pipeline configuration:
 * - testEnvironment system property triggers context.json loading
 * - configKey XML parameter selects browser/language configuration
 */
@Slf4j
public class TestngListener implements ITestListener, IMethodInterceptor, IAnnotationTransformer {

    private static final String METRICS_ENABLED_PROPERTY = "bnc.metrics.enabled";
    private static final String RETRY_ENABLED_PROPERTY = "bnc.test.retry.enabled";

    /**
     * Tracks if context has been loaded for current test suite.
     */
    private static final Map<String, Boolean> contextLoadedForTest = new ConcurrentHashMap<>();

    /**
     * Tracks failed methods per test class for dependency checking.
     * Key: Class name, Value: Map of method names to pass/fail status
     */
    private static final Map<String, Map<String, Boolean>> classMethodResults = new ConcurrentHashMap<>();

    /**
     * Tracks if any test has failed per class (for generic @DependentStep without value).
     * Key: Class name, Value: true if any test failed
     */
    private static final Map<String, Boolean> classHasFailure = new ConcurrentHashMap<>();

    /**
     * Stores the start time of each test for duration calculation.
     */
    private static final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();

    /**
     * Check if metrics collection is enabled.
     */
    private static boolean isMetricsEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(METRICS_ENABLED_PROPERTY));
    }

    /**
     * Check if retry is enabled.
     */
    private static boolean isRetryEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(RETRY_ENABLED_PROPERTY));
    }

    // ==================== IAnnotationTransformer ====================

    /**
     * Apply RetryAnalyzer to all test methods automatically.
     * This eliminates the need to add @Test(retryAnalyzer=...) on each test.
     */
    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (isRetryEnabled() && annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }

    // ==================== IMethodInterceptor ====================

    /**
     * Intercept and reorder test methods to run in lexicographic order.
     * This ensures methods like t001, t002, t003 run in sequence.
     */
    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        List<IMethodInstance> result = new ArrayList<>(methods);

        // Sort by method name lexicographically
        result.sort(Comparator.comparing(m -> m.getMethod().getMethodName()));

        log.info("Test execution order for suite [{}]:", context.getName());
        for (int i = 0; i < result.size(); i++) {
            IMethodInstance method = result.get(i);
            log.info("  {}. {} (class: {})",
                    i + 1,
                    method.getMethod().getMethodName(),
                    method.getMethod().getTestClass().getName());
        }

        return result;
    }

    @Override
    public void onTestStart(ITestResult result) {
        String testKey = getTestKey(result);
        testStartTimes.put(testKey, System.currentTimeMillis());

        // Record metrics
        if (isMetricsEnabled()) {
            MetricsCollector.getInstance().startTest(result);
        }

        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        String className = result.getTestClass().getName();
        String simpleClassName = result.getTestClass().getRealClass().getSimpleName();
        String methodName = method.getName();

        log.info("=".repeat(80));
        log.info("STARTING TEST: {}.{}", className, methodName);

        // Log Xray annotation if present
        Xray xray = result.getTestClass().getRealClass().getAnnotation(Xray.class);
        if (xray != null) {
            log.info("Xray Test Key: {}", xray.test());
            if (!xray.requirement().isEmpty()) {
                log.info("Xray Requirement: {}", xray.requirement());
            }
        }

        // Check for @DependentStep annotation
        DependentStep dependentStep = method.getAnnotation(DependentStep.class);
        if (dependentStep != null) {
            String dependsOn = dependentStep.value();

            if (!dependsOn.isEmpty()) {
                // Specific dependency: skip if that specific method failed
                Map<String, Boolean> classResults = classMethodResults.get(simpleClassName);
                if (classResults != null) {
                    Boolean dependencyResult = classResults.get(dependsOn);
                    if (dependencyResult == null) {
                        log.warn("Dependency method [{}] has not run yet. Test may fail.", dependsOn);
                    } else if (!dependencyResult) {
                        log.error("Dependency method [{}] failed. Skipping test [{}].", dependsOn, methodName);
                        throw new SkipException("Skipped due to failed dependency: " + dependsOn);
                    } else {
                        log.info("Dependency [{}] passed. Proceeding with test.", dependsOn);
                    }
                }
            } else {
                // Generic dependency: skip if ANY previous test in class failed
                Boolean hasFailed = classHasFailure.get(simpleClassName);
                if (hasFailed != null && hasFailed) {
                    log.error("Previous test failed. Skipping dependent test [{}].", methodName);
                    throw new SkipException("Skipped due to previous test failure in class");
                }
            }
        }

        // Log visual checkpoint settings if present
        VisualCheckpoint visualCheckpoint = method.getAnnotation(VisualCheckpoint.class);
        SkipVisualCheck skipVisual = method.getAnnotation(SkipVisualCheck.class);

        if (skipVisual != null) {
            log.info("Visual check SKIPPED for this test. Reason: {}",
                    skipVisual.reason().isEmpty() ? "Not specified" : skipVisual.reason());
        } else if (visualCheckpoint != null) {
            log.info("Visual checkpoint configured: type={}, mismatch={}",
                    visualCheckpoint.type(), visualCheckpoint.onMismatch());
        }

        log.info("=".repeat(80));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        recordMethodResult(result, true);
        long duration = calculateDuration(result);

        // Record metrics
        if (isMetricsEnabled()) {
            MetricsCollector.getInstance().endTest(result);
        }

        log.info("=".repeat(80));
        log.info("TEST PASSED: {}.{} ({}ms)",
                result.getTestClass().getName(),
                result.getMethod().getMethodName(),
                duration);
        log.info("=".repeat(80));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        recordMethodResult(result, false);
        markClassAsFailed(result);
        long duration = calculateDuration(result);

        // Record metrics
        if (isMetricsEnabled()) {
            MetricsCollector.getInstance().endTest(result);
        }

        log.error("=".repeat(80));
        log.error("TEST FAILED: {}.{} ({}ms)",
                result.getTestClass().getName(),
                result.getMethod().getMethodName(),
                duration);

        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            log.error("Failure reason: {}", throwable.getMessage());
            log.error("Stack trace:", throwable);
        }
        log.error("=".repeat(80));
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        recordMethodResult(result, false);

        // Record metrics
        if (isMetricsEnabled()) {
            MetricsCollector.getInstance().endTest(result);
        }

        log.warn("=".repeat(80));
        log.warn("TEST SKIPPED: {}.{}",
                result.getTestClass().getName(),
                result.getMethod().getMethodName());

        Throwable throwable = result.getThrowable();
        if (throwable != null) {
            log.warn("Skip reason: {}", throwable.getMessage());
        }
        log.warn("=".repeat(80));
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        recordMethodResult(result, true);
        log.info("TEST PASSED (within success percentage): {}.{}",
                result.getTestClass().getName(),
                result.getMethod().getMethodName());
    }

    @Override
    public void onStart(ITestContext context) {
        log.info("#".repeat(80));
        log.info("STARTING TEST SUITE: {}", context.getName());
        log.info("Total tests: {}", context.getAllTestMethods().length);
        log.info("#".repeat(80));

        // Load context configuration for pipeline execution
        loadContextConfigIfNeeded(context);

        // Reset failure tracking for new suite
        classHasFailure.clear();

        // Initialize metrics collection
        if (isMetricsEnabled()) {
            MetricsCollector.getInstance().startSuite(context);
            log.info("Metrics collection enabled - reports will be generated at suite completion");
        }
    }

    /**
     * Load context configuration from context.json for pipeline execution.
     * Only loads if testEnvironment system property is set and configKey parameter exists.
     * This is a no-op for local execution (uses debug_config.json instead).
     */
    private void loadContextConfigIfNeeded(ITestContext context) {
        String testEnvironment = System.getProperty("testEnvironment");

        // Skip if not in pipeline mode (no testEnvironment)
        if (testEnvironment == null || testEnvironment.isEmpty()) {
            log.debug("Local execution mode - using debug_config.json");
            return;
        }

        // Get configKey from XML test parameter
        String configKey = context.getCurrentXmlTest().getParameter("configKey");
        if (configKey == null || configKey.isEmpty()) {
            log.warn("Pipeline mode but no configKey parameter found in XML for test '{}'",
                     context.getName());
            return;
        }

        // Check if already loaded for this test
        String testKey = testEnvironment + ":" + configKey;
        if (contextLoadedForTest.containsKey(testKey)) {
            log.debug("Context already loaded for {}", testKey);
            return;
        }

        // Load configuration
        log.info("Pipeline mode: loading context for env='{}', configKey='{}'",
                 testEnvironment, configKey);
        ContextConfigLoader.getInstance().loadConfig(testEnvironment, configKey);
        contextLoadedForTest.put(testKey, true);
    }

    @Override
    public void onFinish(ITestContext context) {
        int passed = context.getPassedTests().size();
        int failed = context.getFailedTests().size();
        int skipped = context.getSkippedTests().size();
        int total = passed + failed + skipped;

        log.info("#".repeat(80));
        log.info("TEST SUITE COMPLETED: {}", context.getName());
        log.info("Results: {} passed, {} failed, {} skipped (total: {})",
                passed, failed, skipped, total);
        log.info("Success rate: {}%",
                total > 0 ? String.format("%.1f", (passed * 100.0) / total) : "N/A");
        log.info("#".repeat(80));

        // Generate reports
        if (isMetricsEnabled()) {
            generateReports(context);
        }

        // Clear results
        classMethodResults.clear();
        classHasFailure.clear();
    }

    /**
     * Generate test reports in all formats (HTML, JSON, CSV).
     */
    private void generateReports(ITestContext context) {
        try {
            TestMetrics metrics = MetricsCollector.getInstance().endSuite(context);
            if (metrics == null) {
                log.warn("No metrics collected - skipping report generation");
                return;
            }

            MetricsReportGenerator generator = new MetricsReportGenerator();
            Map<String, Path> reports = generator.generateReports(metrics);

            log.info("#".repeat(80));
            log.info("TEST REPORTS GENERATED:");
            for (Map.Entry<String, Path> entry : reports.entrySet()) {
                log.info("  {} -> {}", entry.getKey().toUpperCase(), entry.getValue());
            }
            log.info("#".repeat(80));

        } catch (Exception e) {
            log.error("Failed to generate test reports: {}", e.getMessage(), e);
        } finally {
            MetricsCollector.getInstance().reset();
        }
    }

    /**
     * Record the result of a test method for dependency checking.
     */
    private void recordMethodResult(ITestResult result, boolean passed) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        String methodName = result.getMethod().getMethodName();

        classMethodResults.computeIfAbsent(className, k -> new HashMap<>())
                .put(methodName, passed);
    }

    /**
     * Mark the class as having a failure (for generic @DependentStep).
     */
    private void markClassAsFailed(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        classHasFailure.put(className, true);
        log.debug("Class [{}] marked as failed - subsequent @DependentStep tests will be skipped", className);
    }

    /**
     * Calculate test duration.
     */
    private long calculateDuration(ITestResult result) {
        String testKey = getTestKey(result);
        Long startTime = testStartTimes.remove(testKey);
        if (startTime != null) {
            return System.currentTimeMillis() - startTime;
        }
        return result.getEndMillis() - result.getStartMillis();
    }

    /**
     * Generate a unique key for a test result.
     */
    private String getTestKey(ITestResult result) {
        return result.getTestClass().getName() + "#" +
                result.getMethod().getMethodName() + "#" +
                result.hashCode();
    }

    /**
     * Check if a dependency method has passed.
     *
     * @param className  The test class name
     * @param methodName The method to check
     * @return true if passed, false if failed, null if not run yet
     */
    public static Boolean hasMethodPassed(String className, String methodName) {
        Map<String, Boolean> classResults = classMethodResults.get(className);
        return classResults != null ? classResults.get(methodName) : null;
    }

    /**
     * Check if a class has any failures.
     *
     * @param className The test class name
     * @return true if class has failures
     */
    public static boolean hasClassFailed(String className) {
        return classHasFailure.getOrDefault(className, false);
    }

    /**
     * Get all method results for a test class.
     *
     * @param className The test class name
     * @return Map of method names to pass/fail status
     */
    public static Map<String, Boolean> getClassResults(String className) {
        return classMethodResults.getOrDefault(className, Map.of());
    }
}
