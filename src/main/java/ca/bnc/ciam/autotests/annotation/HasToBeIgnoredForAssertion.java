package ca.bnc.ciam.autotests.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field to be ignored during recursive field comparison assertions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HasToBeIgnoredForAssertion {

    /**
     * Optional reason for ignoring the field
     */
    String reason() default "";
}
