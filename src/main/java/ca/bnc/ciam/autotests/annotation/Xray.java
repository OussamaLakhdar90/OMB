package ca.bnc.ciam.autotests.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Links a test class to Xray test case in Jira.
 * Applied at class level - one test per class with steps as sub-items.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Xray {

    /**
     * Xray requirement ID (e.g., "IAME-18976")
     */
    String requirement() default "";

    /**
     * Xray test case ID (e.g., "IAME-21520")
     */
    String test() default "";

    /**
     * Multiple requirement IDs if the test covers multiple requirements
     */
    String[] requirements() default {};

    /**
     * Test execution summary/description
     */
    String summary() default "";

    /**
     * Labels to attach to the test execution
     */
    String[] labels() default {};
}
