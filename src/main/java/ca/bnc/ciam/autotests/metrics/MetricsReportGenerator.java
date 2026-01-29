package ca.bnc.ciam.autotests.metrics;

import ca.bnc.ciam.autotests.metrics.export.CsvMetricsExporter;
import ca.bnc.ciam.autotests.metrics.export.HtmlMetricsExporter;
import ca.bnc.ciam.autotests.metrics.export.JsonMetricsExporter;
import ca.bnc.ciam.autotests.metrics.export.MetricsExporter;
import ca.bnc.ciam.autotests.metrics.export.XmlMetricsExporter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates test metrics reports in multiple formats.
 */
@Slf4j
public class MetricsReportGenerator {

    private static final String DEFAULT_OUTPUT_DIR = "target/metrics";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path outputDirectory;
    private final Map<String, MetricsExporter> exporters = new HashMap<>();

    /**
     * Create generator with default output directory.
     */
    public MetricsReportGenerator() {
        this(DEFAULT_OUTPUT_DIR);
    }

    /**
     * Create generator with custom output directory.
     */
    public MetricsReportGenerator(String outputDir) {
        this.outputDirectory = Paths.get(outputDir);
        registerDefaultExporters();
    }

    /**
     * Register default exporters.
     */
    private void registerDefaultExporters() {
        exporters.put("json", new JsonMetricsExporter());
        exporters.put("csv", new CsvMetricsExporter());
        exporters.put("html", new HtmlMetricsExporter());
        exporters.put("xml", new XmlMetricsExporter());
    }

    /**
     * Register a custom exporter.
     */
    public void registerExporter(String name, MetricsExporter exporter) {
        exporters.put(name, exporter);
    }

    /**
     * Generate reports in all registered formats.
     */
    public Map<String, Path> generateReports(TestMetrics metrics) throws IOException {
        Map<String, Path> generatedFiles = new HashMap<>();

        Files.createDirectories(outputDirectory);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String baseFileName = String.format("test-report_%s_%s",
                sanitizeFileName(metrics.getSuiteName()),
                timestamp);

        for (Map.Entry<String, MetricsExporter> entry : exporters.entrySet()) {
            String format = entry.getKey();
            MetricsExporter exporter = entry.getValue();

            String fileName = baseFileName + "." + exporter.getFileExtension();
            Path filePath = outputDirectory.resolve(fileName);

            try {
                exporter.export(metrics, filePath);
                generatedFiles.put(format, filePath);
                log.info("Generated {} report: {}", format, filePath);
            } catch (Exception e) {
                log.error("Failed to generate {} report", format, e);
            }
        }

        // Also create a "latest" symlink/copy for easy access
        createLatestLinks(baseFileName, generatedFiles);

        return generatedFiles;
    }

    /**
     * Generate report in specific format.
     */
    public Path generateReport(TestMetrics metrics, String format) throws IOException {
        MetricsExporter exporter = exporters.get(format.toLowerCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Unknown report format: " + format);
        }

        Files.createDirectories(outputDirectory);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = String.format("test-report_%s_%s.%s",
                sanitizeFileName(metrics.getSuiteName()),
                timestamp,
                exporter.getFileExtension());

        Path filePath = outputDirectory.resolve(fileName);
        exporter.export(metrics, filePath);

        log.info("Generated {} report: {}", format, filePath);
        return filePath;
    }

    /**
     * Generate report and return as string.
     */
    public String generateReportString(TestMetrics metrics, String format) {
        MetricsExporter exporter = exporters.get(format.toLowerCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Unknown report format: " + format);
        }
        return exporter.exportToString(metrics);
    }

    /**
     * Create "latest" copies for easy access.
     */
    private void createLatestLinks(String baseFileName, Map<String, Path> generatedFiles) {
        for (Map.Entry<String, Path> entry : generatedFiles.entrySet()) {
            try {
                String extension = entry.getValue().getFileName().toString();
                extension = extension.substring(extension.lastIndexOf('.'));

                Path latestPath = outputDirectory.resolve("test-report-latest" + extension);
                Files.deleteIfExists(latestPath);
                Files.copy(entry.getValue(), latestPath);
            } catch (IOException e) {
                log.debug("Could not create latest link for {}", entry.getKey(), e);
            }
        }
    }

    /**
     * Sanitize string for use in file name.
     */
    private String sanitizeFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9_-]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    /**
     * Get output directory.
     */
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Get available formats.
     */
    public java.util.Set<String> getAvailableFormats() {
        return exporters.keySet();
    }

    /**
     * Clean up old reports (older than specified days).
     */
    public int cleanupOldReports(int daysOld) throws IOException {
        if (!Files.exists(outputDirectory)) {
            return 0;
        }

        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        int deleted = 0;

        for (java.io.File file : outputDirectory.toFile().listFiles()) {
            if (file.isFile() && file.lastModified() < cutoffTime) {
                if (file.delete()) {
                    deleted++;
                }
            }
        }

        log.info("Cleaned up {} old report files", deleted);
        return deleted;
    }
}
