package ca.bnc.ciam.autotests.visual.model;

/**
 * Defines the behavior when a visual mismatch is detected.
 */
public enum MismatchBehavior {

    /**
     * Use the default behavior from configuration
     */
    DEFAULT,

    /**
     * Fail the test immediately when mismatch is detected
     */
    FAIL,

    /**
     * Log a warning but continue the test
     */
    WARN,

    /**
     * Silently ignore the mismatch
     */
    IGNORE
}
