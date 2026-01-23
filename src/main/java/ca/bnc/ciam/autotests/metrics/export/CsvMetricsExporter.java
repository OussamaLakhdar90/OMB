package ca.bnc.ciam.autotests.metrics.export;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Exports metrics to CSV format.
 */
@Slf4j
public class CsvMetricsExporter implements MetricsExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DELIMITER = ",";
    private static final String NEWLINE = "\n";

    @Override
    public void export(TestMetrics metrics, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        String csv = exportToString(metrics);
        Files.writeString(outputPath, csv);
        log.info("Metrics exported to CSV: {}", outputPath);
    }

    @Override
    public String exportToString(TestMetrics metrics) {
        StringBuilder sb = new StringBuilder();

        // Summary section
        sb.append("# Test Execution Summary").append(NEWLINE);
        sb.append("Run ID,Suite,Start Time,End Time,Duration (ms),Environment,Browser,Mode,Total,Passed,Failed,Skipped,Pass Rate")
                .append(NEWLINE);
        sb.append(escapeCsv(metrics.getRunId())).append(DELIMITER);
        sb.append(escapeCsv(metrics.getSuiteName())).append(DELIMITER);
        sb.append(metrics.getStartTime().format(DATE_FORMAT)).append(DELIMITER);
        sb.append(metrics.getEndTime() != null ? metrics.getEndTime().format(DATE_FORMAT) : "").append(DELIMITER);
        sb.append(metrics.getDurationMs()).append(DELIMITER);
        sb.append(escapeCsv(metrics.getEnvironment())).append(DELIMITER);
        sb.append(escapeCsv(metrics.getBrowser())).append(DELIMITER);
        sb.append(escapeCsv(metrics.getExecutionMode())).append(DELIMITER);
        sb.append(metrics.getTotalTests()).append(DELIMITER);
        sb.append(metrics.getPassedTests()).append(DELIMITER);
        sb.append(metrics.getFailedTests()).append(DELIMITER);
        sb.append(metrics.getSkippedTests()).append(DELIMITER);
        sb.append(String.format("%.2f", metrics.getPassRate())).append("%");
        sb.append(NEWLINE).append(NEWLINE);

        // Test results section
        sb.append("# Test Results").append(NEWLINE);
        sb.append("Class,Method,Status,Start Time,End Time,Duration (ms),Error Message").append(NEWLINE);

        for (TestMetrics.TestResult result : metrics.getTestResults()) {
            sb.append(escapeCsv(result.getClassName())).append(DELIMITER);
            sb.append(escapeCsv(result.getMethodName())).append(DELIMITER);
            sb.append(result.getStatus()).append(DELIMITER);
            sb.append(result.getStartTime().format(DATE_FORMAT)).append(DELIMITER);
            sb.append(result.getEndTime() != null ? result.getEndTime().format(DATE_FORMAT) : "").append(DELIMITER);
            sb.append(result.getDurationMs()).append(DELIMITER);
            sb.append(escapeCsv(result.getErrorMessage()));
            sb.append(NEWLINE);
        }

        // Visual metrics section if present
        if (!metrics.getVisualMetrics().isEmpty()) {
            sb.append(NEWLINE).append("# Visual Comparison Results").append(NEWLINE);
            sb.append("Test,Baseline,Matched,Diff %,Tolerance %,Status,Comparison Time (ms)")
                    .append(NEWLINE);

            for (TestMetrics.VisualMetric vm : metrics.getVisualMetrics()) {
                sb.append(escapeCsv(vm.getTestName())).append(DELIMITER);
                sb.append(escapeCsv(vm.getBaselineName())).append(DELIMITER);
                sb.append(vm.isMatched()).append(DELIMITER);
                sb.append(String.format("%.4f", vm.getDiffPercentage() * 100)).append(DELIMITER);
                sb.append(String.format("%.4f", vm.getTolerance() * 100)).append(DELIMITER);
                sb.append(vm.getStatus()).append(DELIMITER);
                sb.append(vm.getComparisonTimeMs());
                sb.append(NEWLINE);
            }
        }

        // API metrics section if present
        if (!metrics.getApiMetrics().isEmpty()) {
            sb.append(NEWLINE).append("# API Call Metrics").append(NEWLINE);
            sb.append("Test,Method,Endpoint,Status Code,Response Time (ms),Request Size,Response Size,Success")
                    .append(NEWLINE);

            for (TestMetrics.ApiMetric am : metrics.getApiMetrics()) {
                sb.append(escapeCsv(am.getTestName())).append(DELIMITER);
                sb.append(am.getMethod()).append(DELIMITER);
                sb.append(escapeCsv(am.getEndpoint())).append(DELIMITER);
                sb.append(am.getStatusCode()).append(DELIMITER);
                sb.append(am.getResponseTimeMs()).append(DELIMITER);
                sb.append(am.getRequestSizeBytes()).append(DELIMITER);
                sb.append(am.getResponseSizeBytes()).append(DELIMITER);
                sb.append(am.isSuccess());
                sb.append(NEWLINE);
            }
        }

        return sb.toString();
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    /**
     * Escape CSV value.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains delimiter or quotes
        if (value.contains(DELIMITER) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
