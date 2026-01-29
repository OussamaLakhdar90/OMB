package ca.bnc.ciam.autotests.listener;

import lombok.extern.slf4j.Slf4j;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retry analyzer for failed tests.
 * Automatically retries failed tests once before marking them as failed.
 *
 * <p>This analyzer is applied globally via TestngListener (IAnnotationTransformer),
 * so no need to add {@code @Test(retryAnalyzer = RetryAnalyzer.class)} on each test.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *   <li>System property {@code bnc.test.retry.enabled} - Enable/disable retry (default: true)</li>
 *   <li>System property {@code bnc.test.retry.count} - Max retry attempts (default: 1)</li>
 * </ul>
 *
 * <p>Example to disable retry:</p>
 * <pre>
 * mvn test -Dbnc.test.retry.enabled=false
 * </pre>
 */
@Slf4j
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final String RETRY_ENABLED_PROPERTY = "bnc.test.retry.enabled";
    private static final String RETRY_COUNT_PROPERTY = "bnc.test.retry.count";
    private static final int DEFAULT_MAX_RETRY_COUNT = 1;

    private int retryCount = 0;
    private int maxRetryCount = -1;

    /**
     * Check if retry is enabled.
     */
    private boolean isRetryEnabled() {
        String enabled = System.getProperty(RETRY_ENABLED_PROPERTY);
        // Default is enabled (true) unless explicitly set to false
        return !"false".equalsIgnoreCase(enabled);
    }

    /**
     * Get max retry count from system property or default.
     */
    private int getMaxRetryCount() {
        if (maxRetryCount < 0) {
            String countStr = System.getProperty(RETRY_COUNT_PROPERTY);
            if (countStr != null) {
                try {
                    maxRetryCount = Integer.parseInt(countStr);
                } catch (NumberFormatException e) {
                    maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
                }
            } else {
                maxRetryCount = DEFAULT_MAX_RETRY_COUNT;
            }
        }
        return maxRetryCount;
    }

    @Override
    public boolean retry(ITestResult result) {
        if (!isRetryEnabled()) {
            return false;
        }

        if (retryCount < getMaxRetryCount()) {
            retryCount++;
            String testName = result.getTestClass().getRealClass().getSimpleName() + "." +
                              result.getMethod().getMethodName();

            log.warn("=".repeat(60));
            log.warn("RETRYING TEST: {} (attempt {}/{})",
                     testName, retryCount, getMaxRetryCount());

            Throwable throwable = result.getThrowable();
            if (throwable != null) {
                log.warn("Failure reason: {}", throwable.getMessage());
            }
            log.warn("=".repeat(60));

            return true;
        }

        return false;
    }

    /**
     * Get current retry count for this test.
     */
    public int getRetryCount() {
        return retryCount;
    }
}
