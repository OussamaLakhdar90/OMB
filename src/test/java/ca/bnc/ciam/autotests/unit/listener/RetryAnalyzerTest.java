package ca.bnc.ciam.autotests.unit.listener;

import ca.bnc.ciam.autotests.listener.RetryAnalyzer;
import org.mockito.Mockito;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RetryAnalyzer.
 */
@Test(groups = "unit")
public class RetryAnalyzerTest {

    private RetryAnalyzer retryAnalyzer;
    private ITestResult mockResult;

    @BeforeMethod
    public void setUp() {
        retryAnalyzer = new RetryAnalyzer();
        mockResult = Mockito.mock(ITestResult.class);

        // Setup mock
        org.testng.IClass mockClass = Mockito.mock(org.testng.IClass.class);
        when(mockResult.getTestClass()).thenReturn(mockClass);
        when(mockClass.getRealClass()).thenReturn((Class) RetryAnalyzerTest.class);

        org.testng.ITestNGMethod mockMethod = Mockito.mock(org.testng.ITestNGMethod.class);
        when(mockResult.getMethod()).thenReturn(mockMethod);
        when(mockMethod.getMethodName()).thenReturn("testMethod");
        when(mockResult.getName()).thenReturn("testMethod");

        // Clear system properties
        System.clearProperty("bnc.test.retry.enabled");
        System.clearProperty("bnc.test.retry.count");
    }

    @AfterMethod
    public void tearDown() {
        System.clearProperty("bnc.test.retry.enabled");
        System.clearProperty("bnc.test.retry.count");
    }

    @Test
    public void testRetry_FirstAttempt_ReturnsTrue() {
        boolean result = retryAnalyzer.retry(mockResult);

        assertThat(result).isTrue();
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(1);
    }

    @Test
    public void testRetry_SecondAttempt_ReturnsFalse() {
        // First retry
        retryAnalyzer.retry(mockResult);

        // Second attempt should fail (default max is 1)
        boolean result = retryAnalyzer.retry(mockResult);

        assertThat(result).isFalse();
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(1);
    }

    @Test
    public void testRetry_WhenDisabled_ReturnsFalse() {
        System.setProperty("bnc.test.retry.enabled", "false");

        boolean result = retryAnalyzer.retry(mockResult);

        assertThat(result).isFalse();
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(0);
    }

    @Test
    public void testRetry_WhenExplicitlyEnabled_ReturnsTrue() {
        System.setProperty("bnc.test.retry.enabled", "true");

        boolean result = retryAnalyzer.retry(mockResult);

        assertThat(result).isTrue();
    }

    @Test
    public void testRetry_CustomRetryCount() {
        System.setProperty("bnc.test.retry.count", "2");

        // First retry
        assertThat(retryAnalyzer.retry(mockResult)).isTrue();
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(1);

        // Second retry
        assertThat(retryAnalyzer.retry(mockResult)).isTrue();
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(2);

        // Third attempt should fail (max is 2)
        assertThat(retryAnalyzer.retry(mockResult)).isFalse();
    }

    @Test
    public void testRetry_InvalidRetryCount_UsesDefault() {
        System.setProperty("bnc.test.retry.count", "invalid");

        // First retry should work (default is 1)
        assertThat(retryAnalyzer.retry(mockResult)).isTrue();

        // Second attempt should fail
        assertThat(retryAnalyzer.retry(mockResult)).isFalse();
    }

    @Test
    public void testRetry_ZeroRetryCount() {
        System.setProperty("bnc.test.retry.count", "0");

        // Should not retry
        assertThat(retryAnalyzer.retry(mockResult)).isFalse();
    }

    @Test
    public void testGetRetryCount_InitiallyZero() {
        assertThat(retryAnalyzer.getRetryCount()).isEqualTo(0);
    }
}
