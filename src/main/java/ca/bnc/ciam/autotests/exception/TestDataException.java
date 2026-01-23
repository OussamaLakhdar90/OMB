package ca.bnc.ciam.autotests.exception;

/**
 * Exception thrown when there is an error loading or accessing test data.
 */
public class TestDataException extends RuntimeException {

    public TestDataException(String message) {
        super(message);
    }

    public TestDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public TestDataException(Throwable cause) {
        super(cause);
    }
}
