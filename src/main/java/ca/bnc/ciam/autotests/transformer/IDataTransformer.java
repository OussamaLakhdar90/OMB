package ca.bnc.ciam.autotests.transformer;

/**
 * Interface for data transformation.
 */
public interface IDataTransformer {

    /**
     * Transform a value.
     *
     * @param value The value to transform
     * @return The transformed value
     */
    String transform(String value);
}
