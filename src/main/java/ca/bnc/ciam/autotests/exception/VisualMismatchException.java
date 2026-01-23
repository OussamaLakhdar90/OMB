package ca.bnc.ciam.autotests.exception;

/**
 * Exception thrown when visual comparison detects a mismatch between
 * the actual screenshot and the baseline.
 */
public class VisualMismatchException extends RuntimeException {

    private final String checkpointName;
    private final String baselinePath;
    private final String actualPath;
    private final String diffPath;
    private final double mismatchPercentage;

    public VisualMismatchException(String message) {
        super(message);
        this.checkpointName = null;
        this.baselinePath = null;
        this.actualPath = null;
        this.diffPath = null;
        this.mismatchPercentage = 0;
    }

    public VisualMismatchException(String message, String checkpointName, String baselinePath,
                                   String actualPath, String diffPath, double mismatchPercentage) {
        super(message);
        this.checkpointName = checkpointName;
        this.baselinePath = baselinePath;
        this.actualPath = actualPath;
        this.diffPath = diffPath;
        this.mismatchPercentage = mismatchPercentage;
    }

    public VisualMismatchException(String message, Throwable cause) {
        super(message, cause);
        this.checkpointName = null;
        this.baselinePath = null;
        this.actualPath = null;
        this.diffPath = null;
        this.mismatchPercentage = 0;
    }

    public String getCheckpointName() {
        return checkpointName;
    }

    public String getBaselinePath() {
        return baselinePath;
    }

    public String getActualPath() {
        return actualPath;
    }

    public String getDiffPath() {
        return diffPath;
    }

    public double getMismatchPercentage() {
        return mismatchPercentage;
    }
}
