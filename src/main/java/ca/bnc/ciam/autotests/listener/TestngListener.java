package ca.bnc.ciam.autotests.listener;

import ca.bnc.ciam.autotests.annotation.DependentStep;
import ca.bnc.ciam.autotests.annotation.SkipVisualCheck;
import ca.bnc.ciam.autotests.annotation.VisualCheckpoint;
import ca.bnc.ciam.autotests.annotation.Xray;
import lombok.extern.slf4j.Slf4j;
import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TestNG listener that provides:
 * - Lexicographic ordering of test methods (t001, t002, etc.)
 * - Dependency checking via @DependentStep annotation
 * - Visual checkpoint handling
 * - Test lifecycle logging
 * - Metrics collection hooks
 */
@Slf4j
public class TestngListener implements ITestListener, IMethodInterceptor {

    /**
     * Tracks failed methods per test class for dependency checking.
     * Key: Class name, Value: Set of failed method names
     */
    private static final Map<String, Map<String, Boolean>> classMethodResults = new ConcurrentHashMap<>();

    /**
     * Stores the start time of each test for duration calculation.
     */
    private static final Map<String, Long> testStartTimes = new ConcurrentHashMap<>();

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

        Method method = result.getMethod().getConstructorOrMethod().getMethod();
        String className = result.getTestClass().getName();
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

        // Check for @DependentStep and verify dependency passed
        DependentStep dependentStep = method.getAnnotation(DependentStep.class);
        if (dependentStep != null) {
            String dependsOn = dependentStep.value();
            if (!dependsOn.isEmpty()) {
                String simpleClassName = result.getTestClass().getRealClass().getSimpleName();
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
        long duration = calculateDuration(result);

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

        // Clear results for this test class
        classMethodResults.clear();
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
     * Get all method results for a test class.
     *
     * @param className The test class name
     * @return Map of method names to pass/fail status
     */
    public static Map<String, Boolean> getClassResults(String className) {
        return classMethodResults.getOrDefault(className, Map.of());
    }
}
