package ca.bnc.ciam.autotests.annotation;

import ca.bnc.ciam.autotests.visual.model.MismatchBehavior;
import ca.bnc.ciam.autotests.visual.model.ScreenshotType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures visual checkpoint settings for a test method.
 * Allows customization of screenshot type, element selector, and mismatch behavior.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VisualCheckpoint {

    /**
     * Name of the checkpoint (used for baseline file naming)
     */
    String name() default "";

    /**
     * Type of screenshot to capture
     */
    ScreenshotType type() default ScreenshotType.FULL_PAGE;

    /**
     * CSS selector for element screenshot (only used when type = ELEMENT)
     */
    String selector() default "";

    /**
     * Behavior when visual mismatch is detected
     */
    MismatchBehavior onMismatch() default MismatchBehavior.DEFAULT;

    /**
     * Custom tolerance for this checkpoint (overrides global setting)
     * Value between 0.0 and 1.0
     */
    double tolerance() default -1.0;

    /**
     * CSS selectors for regions to ignore during comparison
     */
    String[] ignoreRegions() default {};
}
