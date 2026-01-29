package ca.bnc.ciam.autotests.visual;

import ai.djl.Application;
import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.transform.Normalize;
import ai.djl.modality.cv.transform.Resize;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * AI-based image comparison using DJL (Deep Java Library) and ResNet18.
 *
 * This comparator uses a pre-trained ResNet18 neural network to extract
 * feature vectors from images, then compares them using cosine similarity.
 * This approach is more robust to minor visual differences like anti-aliasing,
 * font rendering variations, and dynamic content.
 *
 * The model is automatically downloaded on first use (~45MB) and cached locally.
 *
 * Usage:
 * <pre>
 * AIImageComparator comparator = new AIImageComparator();
 * AIComparisonResult result = comparator.compare(baselineImage, actualImage);
 * if (result.isMatch()) {
 *     // Images are perceptually similar
 * }
 * </pre>
 */
@Slf4j
public class AIImageComparator implements AutoCloseable {

    private static final float[] IMAGENET_MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] IMAGENET_STD = {0.229f, 0.224f, 0.225f};
    private static final int INPUT_SIZE = 224;

    private final double defaultThreshold;
    private ZooModel<Image, float[]> model;
    private Predictor<Image, float[]> predictor;
    private boolean initialized = false;
    private String initError = null;

    /**
     * Create comparator with default similarity threshold of 0.95 (95%).
     */
    public AIImageComparator() {
        this(0.95);
    }

    /**
     * Create comparator with custom similarity threshold.
     *
     * @param threshold similarity threshold (0.0 to 1.0), higher = stricter matching
     */
    public AIImageComparator(double threshold) {
        this.defaultThreshold = threshold;
        initialize();
    }

    /** System property for specifying local model path */
    public static final String MODEL_PATH_PROPERTY = "bnc.visual.ai.model.path";

    /** System property for specifying model URL (direct download) */
    public static final String MODEL_URL_PROPERTY = "bnc.visual.ai.model.url";

    /** Default DJL model download URL */
    private static final String DEFAULT_MODEL_URL =
            "https://djl-ai.s3.amazonaws.com/mlrepo/model/cv/image_classification/ai/djl/pytorch/resnet/0.0.1/traced_resnet18.pt.gz";

    /**
     * Initialize the DJL model lazily.
     * Tries loading in this order:
     * 1. Local model path (if bnc.visual.ai.model.path is set)
     * 2. Direct URL download (if bnc.visual.ai.model.url is set)
     * 3. Standard DJL model zoo (requires internet access)
     */
    private synchronized void initialize() {
        if (initialized) {
            return;
        }

        try {
            log.info("Initializing AI image comparator with ResNet18...");

            // Check for local model path first
            String localModelPath = System.getProperty(MODEL_PATH_PROPERTY);
            String modelUrl = System.getProperty(MODEL_URL_PROPERTY);

            Criteria<Image, float[]> criteria;

            if (localModelPath != null && !localModelPath.isEmpty()) {
                // Load from local file
                Path modelPath = Paths.get(localModelPath);
                if (Files.exists(modelPath)) {
                    log.info("Loading AI model from local path: {}", localModelPath);
                    criteria = Criteria.builder()
                            .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                            .setTypes(Image.class, float[].class)
                            .optModelPath(modelPath)
                            .optTranslator(new FeatureExtractionTranslator())
                            .build();
                } else {
                    throw new IOException("Local model file not found: " + localModelPath);
                }
            } else if (modelUrl != null && !modelUrl.isEmpty()) {
                // Load from direct URL
                log.info("Loading AI model from URL: {}", modelUrl);
                criteria = Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(Image.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optTranslator(new FeatureExtractionTranslator())
                        .build();
            } else {
                // Standard model zoo (requires internet)
                log.info("Loading AI model from DJL model zoo (requires internet)...");
                criteria = Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(Image.class, float[].class)
                        .optArtifactId("resnet")
                        .optFilter("layers", "18")
                        .optTranslator(new FeatureExtractionTranslator())
                        .build();
            }

            model = criteria.loadModel();
            predictor = model.newPredictor();
            initialized = true;

            log.info("AI image comparator initialized successfully");

        } catch (ModelNotFoundException | MalformedModelException | IOException e) {
            initError = e.getMessage();
            log.error("Failed to initialize AI image comparator: {}", e.getMessage());
            log.warn("AI comparison will be unavailable. Falling back to pixel-based comparison.");
            log.warn("");
            log.warn("=== MANUAL MODEL INSTALLATION ===");
            log.warn("If behind a firewall, download the model manually:");
            log.warn("1. Download: {}", DEFAULT_MODEL_URL);
            log.warn("2. Extract to a local directory");
            log.warn("3. Set system property: -D{}=/path/to/model", MODEL_PATH_PROPERTY);
            log.warn("Or specify a direct URL: -D{}=<url>", MODEL_URL_PROPERTY);
            log.warn("================================");
        }
    }

    /**
     * Custom translator for extracting feature vectors from images.
     */
    private static class FeatureExtractionTranslator implements Translator<Image, float[]> {

        private final Pipeline pipeline;

        public FeatureExtractionTranslator() {
            this.pipeline = new Pipeline()
                    .add(new Resize(INPUT_SIZE, INPUT_SIZE))
                    .add(new ToTensor())
                    .add(new Normalize(IMAGENET_MEAN, IMAGENET_STD));
        }

        @Override
        public NDList processInput(TranslatorContext ctx, Image input) {
            NDArray array = input.toNDArray(ctx.getNDManager(), Image.Flag.COLOR);
            return pipeline.transform(new NDList(array));
        }

        @Override
        public float[] processOutput(TranslatorContext ctx, NDList list) {
            // Get the feature vector from the model output
            NDArray output = list.singletonOrThrow();
            // Flatten to 1D array if needed
            if (output.getShape().dimension() > 1) {
                output = output.flatten();
            }
            return output.toFloatArray();
        }

        @Override
        public Batchifier getBatchifier() {
            return Batchifier.STACK;
        }
    }

    /**
     * Check if the AI comparator is available.
     */
    public boolean isAvailable() {
        return initialized && predictor != null;
    }

    /**
     * Get initialization error if any.
     */
    public String getInitError() {
        return initError;
    }

    /**
     * Compare two images using AI-based feature extraction.
     *
     * @param baseline the baseline image
     * @param actual the actual image to compare
     * @return comparison result with similarity score
     */
    public AIComparisonResult compare(BufferedImage baseline, BufferedImage actual) {
        return compare(baseline, actual, defaultThreshold);
    }

    /**
     * Compare two images with custom similarity threshold.
     *
     * @param baseline the baseline image
     * @param actual the actual image to compare
     * @param threshold similarity threshold (0.0 to 1.0)
     * @return comparison result
     */
    public AIComparisonResult compare(BufferedImage baseline, BufferedImage actual, double threshold) {
        if (!isAvailable()) {
            log.warn("AI comparator not available: {}", initError);
            return AIComparisonResult.builder()
                    .match(false)
                    .similarity(0.0)
                    .threshold(threshold)
                    .error("AI comparator not initialized: " + initError)
                    .build();
        }

        try {
            // Convert BufferedImages to DJL Images
            Image baselineImg = bufferedImageToDjlImage(baseline);
            Image actualImg = bufferedImageToDjlImage(actual);

            // Extract feature vectors
            float[] baselineFeatures = predictor.predict(baselineImg);
            float[] actualFeatures = predictor.predict(actualImg);

            // Calculate cosine similarity
            double similarity = cosineSimilarity(baselineFeatures, actualFeatures);
            boolean match = similarity >= threshold;

            log.info("AI image comparison: similarity={:.4f}, threshold={:.4f}, match={}",
                    similarity, threshold, match);

            return AIComparisonResult.builder()
                    .match(match)
                    .similarity(similarity)
                    .threshold(threshold)
                    .baselineFeatureSize(baselineFeatures.length)
                    .actualFeatureSize(actualFeatures.length)
                    .build();

        } catch (TranslateException | IOException e) {
            log.error("AI comparison failed: {}", e.getMessage());
            return AIComparisonResult.builder()
                    .match(false)
                    .similarity(0.0)
                    .threshold(threshold)
                    .error(e.getMessage())
                    .build();
        }
    }

    /**
     * Convert BufferedImage to DJL Image.
     */
    private Image bufferedImageToDjlImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    /**
     * Calculate cosine similarity between two feature vectors.
     *
     * Cosine similarity = (A . B) / (||A|| * ||B||)
     * Returns value between -1 and 1, where 1 means identical.
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Feature vectors must have same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    @Override
    public void close() {
        if (predictor != null) {
            predictor.close();
            predictor = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
        initialized = false;
    }

    /**
     * AI comparison result data class.
     */
    @Data
    @Builder
    public static class AIComparisonResult {
        private boolean match;
        private double similarity;
        private double threshold;
        private int baselineFeatureSize;
        private int actualFeatureSize;
        private String error;

        public boolean hasError() {
            return error != null && !error.isEmpty();
        }

        public String getSummary() {
            if (hasError()) {
                return String.format("AI Match: ERROR - %s", error);
            }
            return String.format("AI Match: %s, Similarity: %.4f, Threshold: %.4f, Features: %d",
                    match, similarity, threshold, baselineFeatureSize);
        }
    }
}
