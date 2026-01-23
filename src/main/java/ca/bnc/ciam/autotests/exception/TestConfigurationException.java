package ca.bnc.ciam.autotests.exception;

/**
 * Exception thrown when there is a configuration error in the test framework.
 */
public class TestConfigurationException extends RuntimeException {

    public TestConfigurationException(String message) {
        super(message);
    }

    public TestConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestConfigurationException(Throwable cause) {
        super(cause);
    }
}
