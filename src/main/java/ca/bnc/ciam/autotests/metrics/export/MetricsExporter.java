package ca.bnc.ciam.autotests.metrics.export;

import ca.bnc.ciam.autotests.metrics.TestMetrics;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for metrics exporters.
 */
public interface MetricsExporter {

    /**
     * Export metrics to file.
     *
     * @param metrics The metrics to export
     * @param outputPath The output file path
     * @throws IOException If export fails
     */
    void export(TestMetrics metrics, Path outputPath) throws IOException;

    /**
     * Export metrics to string.
     *
     * @param metrics The metrics to export
     * @return The formatted metrics string
     */
    String exportToString(TestMetrics metrics);

    /**
     * Get the file extension for this exporter.
     *
     * @return File extension (e.g., "json", "csv", "html")
     */
    String getFileExtension();
}
