package ca.bnc.ciam.autotests.metrics.export;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Exports metrics to JSON format.
 */
@Slf4j
public class JsonMetricsExporter implements MetricsExporter {

    private final ObjectMapper objectMapper;

    public JsonMetricsExporter() {
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Override
    public void export(TestMetrics metrics, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        objectMapper.writeValue(outputPath.toFile(), metrics);
        log.info("Metrics exported to JSON: {}", outputPath);
    }

    @Override
    public String exportToString(TestMetrics metrics) {
        try {
            return objectMapper.writeValueAsString(metrics);
        } catch (Exception e) {
            log.error("Failed to export metrics to JSON string", e);
            throw new RuntimeException("Failed to export metrics", e);
        }
    }

    @Override
    public String getFileExtension() {
        return "json";
    }
}
