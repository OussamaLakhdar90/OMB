package ca.bnc.ciam.autotests.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test method to skip automatic visual screenshot capture.
 * Use this for steps with dynamic content that should not be visually validated.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SkipVisualCheck {

    /**
     * Optional reason for skipping visual check
     */
    String reason() default "";
}
