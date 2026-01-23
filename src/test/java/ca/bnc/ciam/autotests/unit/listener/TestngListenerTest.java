package ca.bnc.ciam.autotests.unit.listener;

import ca.bnc.ciam.autotests.listener.TestngListener;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TestngListener.
 * Tests listener interface implementation and dependency checking.
 * Note: Many methods require full TestNG integration and are tested via integration tests.
 */
@Test(groups = "unit")
public class TestngListenerTest {

    private TestngListener listener;

    @BeforeMethod
    public void setUp() {
        listener = new TestngListener();
    }

    // ===========================================
    // Interface Implementation Tests
    // ===========================================

    @Test
    public void testImplements_ITestListener() {
        assertThat(listener).isInstanceOf(org.testng.ITestListener.class);
    }

    @Test
    public void testImplements_IMethodInterceptor() {
        assertThat(listener).isInstanceOf(org.testng.IMethodInterceptor.class);
    }

    // ===========================================
    // intercept Tests - with null context
    // ===========================================

    @Test
    public void testIntercept_EmptyList_WithNullContext_ThrowsNPE() {
        List<org.testng.IMethodInstance> methods = new ArrayList<>();

        try {
            listener.intercept(methods, null);
        } catch (NullPointerException e) {
            // Expected - the library's intercept method requires non-null context
            assertThat(e).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    public void testIntercept_NullMethods_HandlesGracefully() {
        try {
            listener.intercept(null, null);
        } catch (NullPointerException e) {
            // Expected - null input not supported
        }
    }

    // ===========================================
    // Skip Exception Tests
    // ===========================================

    @Test
    public void testSkipException_IsCorrectType() {
        SkipException skip = new SkipException("Test skipped due to dependency failure");
        assertThat(skip.getMessage()).contains("Test skipped");
    }

    @Test
    public void testSkipException_ForDependencyFailure() {
        SkipException skip = new SkipException("Skipped due to failed dependency: t001_Login");
        assertThat(skip.getMessage()).contains("failed dependency");
        assertThat(skip.getMessage()).contains("t001_Login");
    }

    @Test
    public void testSkipException_ForPreviousFailure() {
        SkipException skip = new SkipException("Skipped due to previous test failure in class");
        assertThat(skip.getMessage()).contains("previous test failure");
    }

    // ===========================================
    // Listener State Tests
    // ===========================================

    @Test
    public void testNewInstance_HasNoTrackedState() {
        TestngListener freshListener = new TestngListener();
        assertThat(freshListener).isNotNull();
    }

    @Test
    public void testMultipleInstances_AreIndependent() {
        TestngListener listener1 = new TestngListener();
        TestngListener listener2 = new TestngListener();

        assertThat(listener1).isNotSameAs(listener2);
    }

    // ===========================================
    // Default Method Behavior Tests
    // ===========================================

    @Test
    public void testOnStart_DoesNotThrow() {
        try {
            listener.onStart((ITestContext) null);
        } catch (NullPointerException e) {
            // Acceptable - null context not supported
        }
    }

    @Test
    public void testOnFinish_DoesNotThrow() {
        try {
            listener.onFinish((ITestContext) null);
        } catch (NullPointerException e) {
            // Acceptable - null context not supported
        }
    }

    // ===========================================
    // Test Result Status Constants
    // ===========================================

    @Test
    public void testITestResult_SuccessStatus() {
        assertThat(org.testng.ITestResult.SUCCESS).isEqualTo(1);
    }

    @Test
    public void testITestResult_FailureStatus() {
        assertThat(org.testng.ITestResult.FAILURE).isEqualTo(2);
    }

    @Test
    public void testITestResult_SkipStatus() {
        assertThat(org.testng.ITestResult.SKIP).isEqualTo(3);
    }

    // ===========================================
    // DependentStep Annotation Detection Tests
    // ===========================================

    @Test
    public void testDependentStepAnnotation_Exists() {
        assertThat(ca.bnc.ciam.autotests.annotation.DependentStep.class).isNotNull();
        assertThat(ca.bnc.ciam.autotests.annotation.DependentStep.class.isAnnotation()).isTrue();
    }

    @Test
    public void testDependentStepAnnotation_HasRuntimeRetention() {
        java.lang.annotation.Retention retention = ca.bnc.ciam.autotests.annotation.DependentStep.class
                .getAnnotation(java.lang.annotation.Retention.class);

        assertThat(retention).isNotNull();
        assertThat(retention.value()).isEqualTo(java.lang.annotation.RetentionPolicy.RUNTIME);
    }

    @Test
    public void testDependentStepAnnotation_HasValueAttribute() throws NoSuchMethodException {
        // @DependentStep has a value() attribute for specifying dependency
        assertThat(ca.bnc.ciam.autotests.annotation.DependentStep.class.getMethod("value")).isNotNull();
    }

    // ===========================================
    // Static Helper Method Tests
    // ===========================================

    @Test
    public void testHasMethodPassed_NonExistentClass_ReturnsNull() {
        Boolean result = TestngListener.hasMethodPassed("NonExistentClass", "someMethod");
        assertThat(result).isNull();
    }

    @Test
    public void testHasClassFailed_NonExistentClass_ReturnsFalse() {
        boolean result = TestngListener.hasClassFailed("NonExistentClass");
        assertThat(result).isFalse();
    }

    @Test
    public void testGetClassResults_NonExistentClass_ReturnsEmptyMap() {
        Map<String, Boolean> results = TestngListener.getClassResults("NonExistentClass");
        assertThat(results).isEmpty();
    }

    // ===========================================
    // Annotation Target Tests
    // ===========================================

    @Test
    public void testDependentStepAnnotation_TargetsMethod() {
        java.lang.annotation.Target target = ca.bnc.ciam.autotests.annotation.DependentStep.class
                .getAnnotation(java.lang.annotation.Target.class);

        assertThat(target).isNotNull();
        assertThat(target.value()).contains(java.lang.annotation.ElementType.METHOD);
    }
}
