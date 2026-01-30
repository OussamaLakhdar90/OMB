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

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    /** Embedded model resource path */
    private static final String EMBEDDED_MODEL_RESOURCE = "/models/resnet18/traced_resnet18.pt";

    /** Embedded JNI library resource path (platform-specific) */
    private static final String EMBEDDED_JNI_RESOURCE = "/native/win-x86_64/djl_torch.dll";

    /** PyTorch and DJL versions for cache directory naming */
    private static final String PYTORCH_VERSION = "2.1.1";
    private static final String DJL_VERSION = "0.27.0";

    /** Static flag to track DJL availability - checked once at class load */
    private static final boolean DJL_AVAILABLE;
    private static final String DJL_UNAVAILABLE_REASON;

    static {
        // Set DJL cache directory to system temp if not already set
        // This fixes "Failed to save pytorch index file" errors on systems where home directory is read-only
        // See: https://docs.djl.ai/master/docs/development/cache_management.html
        String djlCacheDir;
        if (System.getProperty("DJL_CACHE_DIR") == null && System.getenv("DJL_CACHE_DIR") == null) {
            String tempDir = System.getProperty("java.io.tmpdir");
            djlCacheDir = tempDir + java.io.File.separator + "djl-cache";
            System.setProperty("DJL_CACHE_DIR", djlCacheDir);
        } else {
            djlCacheDir = System.getProperty("DJL_CACHE_DIR", System.getenv("DJL_CACHE_DIR"));
        }

        // Extract embedded JNI library to cache directory (prevents download at runtime)
        // This is needed for corporate environments with SSL interception
        extractEmbeddedJniLibrary(djlCacheDir);

        boolean available = false;
        String reason = null;
        try {
            // Check if core DJL classes are available using reflection
            Class.forName("ai.djl.repository.zoo.Criteria");
            Class.forName("ai.djl.inference.Predictor");
            Class.forName("ai.djl.translate.TranslateException");
            Class.forName("ai.djl.pytorch.engine.PtEngine");
            available = true;
        } catch (ClassNotFoundException e) {
            reason = "DJL classes not found: " + e.getMessage();
        } catch (NoClassDefFoundError e) {
            reason = "DJL dependency missing: " + e.getMessage();
        } catch (Exception e) {
            reason = "DJL check failed: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        DJL_AVAILABLE = available;
        DJL_UNAVAILABLE_REASON = reason;
    }

    /**
     * Extract embedded JNI library to DJL cache directory.
     * DJL expects the library at: {cache}/pytorch/{version}-cpu-{platform}/{djl_version}-djl_torch.dll
     */
    private static void extractEmbeddedJniLibrary(String cacheDir) {
        // Only extract for Windows x86_64 (the embedded DLL is platform-specific)
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        if (!os.contains("win") || (!arch.contains("amd64") && !arch.contains("x86_64"))) {
            return; // Not Windows x86_64, skip extraction
        }

        try {
            // Check if embedded JNI library exists
            URL jniUrl = AIImageComparator.class.getResource(EMBEDDED_JNI_RESOURCE);
            if (jniUrl == null) {
                return; // No embedded library, DJL will try to download
            }

            // DJL cache structure: {cache}/pytorch/{pytorch_version}-cpu-win-x86_64/
            Path pytorchCacheDir = Paths.get(cacheDir, "pytorch", PYTORCH_VERSION + "-cpu-win-x86_64");
            Files.createDirectories(pytorchCacheDir);

            // The JNI library file name: {djl_version}-djl_torch.dll
            Path jniFile = pytorchCacheDir.resolve(DJL_VERSION + "-djl_torch.dll");

            // Only extract if not already present
            if (!Files.exists(jniFile)) {
                try (InputStream is = AIImageComparator.class.getResourceAsStream(EMBEDDED_JNI_RESOURCE)) {
                    if (is != null) {
                        Files.copy(is, jniFile, StandardCopyOption.REPLACE_EXISTING);
                        // Log at debug level to avoid noise
                    }
                }
            }
        } catch (Exception e) {
            // Silently ignore - DJL will try to download if extraction fails
        }
    }

    /**
     * Check if DJL is available in the classpath.
     * @return true if DJL classes are present
     */
    public static boolean isDjlAvailable() {
        return DJL_AVAILABLE;
    }

    /**
     * Initialize the DJL model lazily.
     * Tries loading in this order:
     * 1. Embedded model from classpath (bundled with library)
     * 2. Local model path (if bnc.visual.ai.model.path is set)
     * 3. Direct URL download (if bnc.visual.ai.model.url is set)
     * 4. Standard DJL model zoo (requires internet access)
     */
    private synchronized void initialize() {
        if (initialized) {
            return;
        }

        // First check if DJL is available
        if (!DJL_AVAILABLE) {
            initError = DJL_UNAVAILABLE_REASON;
            log.warn("DJL is not available: {}. AI comparison will be disabled.", DJL_UNAVAILABLE_REASON);
            return;
        }

        try {
            log.info("Initializing AI image comparator with ResNet18...");

            // Check for local model path override
            String localModelPath = System.getProperty(MODEL_PATH_PROPERTY);
            String modelUrl = System.getProperty(MODEL_URL_PROPERTY);

            Criteria<Image, float[]> criteria = null;

            // 1. Try embedded model from classpath first
            Path embeddedModelPath = extractEmbeddedModel();
            if (embeddedModelPath != null) {
                log.info("Loading AI model from embedded resource");
                criteria = Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(Image.class, float[].class)
                        .optModelPath(embeddedModelPath)
                        .optTranslator(new FeatureExtractionTranslator())
                        .build();
            }
            // 2. Try local file path
            else if (localModelPath != null && !localModelPath.isEmpty()) {
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
            }
            // 3. Try direct URL
            else if (modelUrl != null && !modelUrl.isEmpty()) {
                log.info("Loading AI model from URL: {}", modelUrl);
                criteria = Criteria.builder()
                        .optApplication(Application.CV.IMAGE_CLASSIFICATION)
                        .setTypes(Image.class, float[].class)
                        .optModelUrls(modelUrl)
                        .optTranslator(new FeatureExtractionTranslator())
                        .build();
            }
            // 4. Fall back to model zoo (requires internet)
            else {
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
        } catch (Throwable e) {
            // Catch any other exceptions AND errors (TranslateException, NoClassDefFoundError, etc.)
            // NoClassDefFoundError is an Error, not Exception, so we need to catch Throwable
            initError = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("Failed to initialize AI image comparator: {}", initError, e);
            log.warn("AI comparison will be unavailable. Falling back to pixel-based comparison.");
        }
    }

    /**
     * Extract embedded model from classpath to temp directory.
     * Returns null if embedded model is not available.
     *
     * DJL expects the model file name to match the directory name + ".pt" extension.
     * For example: directory "resnet18" should contain "resnet18.pt"
     */
    private Path extractEmbeddedModel() {
        try {
            log.debug("Looking for embedded model at: {}", EMBEDDED_MODEL_RESOURCE);
            URL resourceUrl = getClass().getResource(EMBEDDED_MODEL_RESOURCE);
            if (resourceUrl == null) {
                log.warn("Embedded model not found in classpath: {}", EMBEDDED_MODEL_RESOURCE);
                return null;
            }
            log.debug("Found embedded model at URL: {}", resourceUrl);

            // Create temp directory for extracted model
            // Use fixed name "resnet18" so DJL can find "resnet18.pt" inside it
            Path tempDir = Files.createTempDirectory("djl-resnet18-");
            tempDir.toFile().deleteOnExit();

            // DJL expects model file name to match directory pattern
            // Extract directory name and create matching .pt file
            String dirName = tempDir.getFileName().toString();
            Path modelFile = tempDir.resolve(dirName + ".pt");
            modelFile.toFile().deleteOnExit();

            // Extract model from classpath to temp file
            try (InputStream is = getClass().getResourceAsStream(EMBEDDED_MODEL_RESOURCE)) {
                if (is == null) {
                    return null;
                }
                Files.copy(is, modelFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Extracted embedded model to: {}", modelFile);
            return tempDir;

        } catch (IOException e) {
            log.warn("Failed to extract embedded model: {}", e.getMessage());
            return null;
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
