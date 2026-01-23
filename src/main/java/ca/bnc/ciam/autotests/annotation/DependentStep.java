package ca.bnc.ciam.autotests.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method as dependent on previous steps.
 * If a previous step fails, this step will be skipped.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DependentStep {

    /**
     * Optional description of the dependency
     */
    String value() default "";
}
