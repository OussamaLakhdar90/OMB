package ca.bnc.ciam.autotests.metrics.export;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Exports metrics to HTML format with embedded styling.
 */
@Slf4j
public class HtmlMetricsExporter implements MetricsExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void export(TestMetrics metrics, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        String html = exportToString(metrics);
        Files.writeString(outputPath, html);
        log.info("Metrics exported to HTML: {}", outputPath);
    }

    @Override
    public String exportToString(TestMetrics metrics) {
        StringBuilder html = new StringBuilder();

        html.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Test Execution Report - %s</title>
                <style>
                    :root {
                        --success: #28a745;
                        --danger: #dc3545;
                        --warning: #ffc107;
                        --info: #17a2b8;
                        --dark: #343a40;
                        --light: #f8f9fa;
                    }
                    * { box-sizing: border-box; }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background: #f5f5f5;
                        color: #333;
                    }
                    .container { max-width: 1400px; margin: 0 auto; }
                    .card {
                        background: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        margin-bottom: 20px;
                        padding: 20px;
                    }
                    h1, h2, h3 { margin-top: 0; color: var(--dark); }
                    h1 { font-size: 24px; }
                    h2 { font-size: 18px; border-bottom: 2px solid var(--info); padding-bottom: 10px; }
                    .summary-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 15px;
                        margin-bottom: 20px;
                    }
                    .metric-box {
                        background: var(--light);
                        padding: 15px;
                        border-radius: 6px;
                        text-align: center;
                    }
                    .metric-value {
                        font-size: 28px;
                        font-weight: bold;
                        color: var(--dark);
                    }
                    .metric-label { color: #666; font-size: 14px; }
                    .success { color: var(--success); }
                    .danger { color: var(--danger); }
                    .warning { color: var(--warning); }
                    table {
                        width: 100%%;
                        border-collapse: collapse;
                        font-size: 14px;
                    }
                    th, td {
                        padding: 12px;
                        text-align: left;
                        border-bottom: 1px solid #ddd;
                    }
                    th { background: var(--dark); color: white; }
                    tr:hover { background: #f5f5f5; }
                    .status {
                        padding: 4px 12px;
                        border-radius: 4px;
                        font-weight: bold;
                        font-size: 12px;
                    }
                    .status-passed { background: #d4edda; color: var(--success); }
                    .status-failed { background: #f8d7da; color: var(--danger); }
                    .status-skipped { background: #fff3cd; color: #856404; }
                    .progress-bar {
                        height: 24px;
                        background: #e9ecef;
                        border-radius: 4px;
                        overflow: hidden;
                        display: flex;
                    }
                    .progress-passed { background: var(--success); }
                    .progress-failed { background: var(--danger); }
                    .progress-skipped { background: var(--warning); }
                    .error-msg {
                        max-width: 300px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        color: var(--danger);
                    }
                    .timestamp { color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
            """.formatted(escapeHtml(metrics.getSuiteName())));

        // Header
        html.append("""
            <div class="card">
                <h1>Test Execution Report</h1>
                <p class="timestamp">Generated: %s | Run ID: %s</p>
            </div>
            """.formatted(DATE_FORMAT.format(metrics.getEndTime()), escapeHtml(metrics.getRunId())));

        // Summary metrics
        html.append("<div class=\"card\"><h2>Summary</h2><div class=\"summary-grid\">");

        addMetricBox(html, "Total Tests", String.valueOf(metrics.getTotalTests()), "");
        addMetricBox(html, "Passed", String.valueOf(metrics.getPassedTests()), "success");
        addMetricBox(html, "Failed", String.valueOf(metrics.getFailedTests()), "danger");
        addMetricBox(html, "Skipped", String.valueOf(metrics.getSkippedTests()), "warning");
        addMetricBox(html, "Pass Rate", String.format("%.1f%%", metrics.getPassRate()),
                metrics.getPassRate() >= 90 ? "success" : (metrics.getPassRate() >= 70 ? "warning" : "danger"));
        addMetricBox(html, "Duration", formatDuration(metrics.getDurationMs()), "");
        addMetricBox(html, "Environment", metrics.getEnvironment(), "");
        addMetricBox(html, "Browser", metrics.getBrowser(), "");

        html.append("</div>");

        // Progress bar
        double passedPct = metrics.getTotalTests() > 0 ? (metrics.getPassedTests() * 100.0 / metrics.getTotalTests()) : 0;
        double failedPct = metrics.getTotalTests() > 0 ? (metrics.getFailedTests() * 100.0 / metrics.getTotalTests()) : 0;
        double skippedPct = metrics.getTotalTests() > 0 ? (metrics.getSkippedTests() * 100.0 / metrics.getTotalTests()) : 0;

        html.append("""
            <div class="progress-bar">
                <div class="progress-passed" style="width: %.1f%%"></div>
                <div class="progress-failed" style="width: %.1f%%"></div>
                <div class="progress-skipped" style="width: %.1f%%"></div>
            </div>
            </div>
            """.formatted(passedPct, failedPct, skippedPct));

        // Test results table
        html.append("""
            <div class="card">
                <h2>Test Results</h2>
                <table>
                    <thead>
                        <tr>
                            <th>Class</th>
                            <th>Method</th>
                            <th>Status</th>
                            <th>Duration</th>
                            <th>Error</th>
                        </tr>
                    </thead>
                    <tbody>
            """);

        for (TestMetrics.TestResult result : metrics.getTestResults()) {
            String statusClass = switch (result.getStatus()) {
                case PASSED -> "status-passed";
                case FAILED -> "status-failed";
                case SKIPPED -> "status-skipped";
            };

            html.append("""
                <tr>
                    <td>%s</td>
                    <td>%s</td>
                    <td><span class="status %s">%s</span></td>
                    <td>%s</td>
                    <td class="error-msg" title="%s">%s</td>
                </tr>
                """.formatted(
                    escapeHtml(getSimpleClassName(result.getClassName())),
                    escapeHtml(result.getMethodName()),
                    statusClass,
                    result.getStatus(),
                    formatDuration(result.getDurationMs()),
                    escapeHtml(result.getErrorMessage()),
                    escapeHtml(truncate(result.getErrorMessage(), 50))
            ));
        }

        html.append("</tbody></table></div>");

        // Visual metrics if present
        if (!metrics.getVisualMetrics().isEmpty()) {
            html.append("""
                <div class="card">
                    <h2>Visual Comparison Results</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Test</th>
                                <th>Baseline</th>
                                <th>Status</th>
                                <th>Diff %%</th>
                                <th>Tolerance %%</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

            for (TestMetrics.VisualMetric vm : metrics.getVisualMetrics()) {
                String statusClass = vm.isMatched() ? "status-passed" : "status-failed";
                html.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td><span class="status %s">%s</span></td>
                        <td>%.4f%%</td>
                        <td>%.4f%%</td>
                    </tr>
                    """.formatted(
                        escapeHtml(vm.getTestName()),
                        escapeHtml(vm.getBaselineName()),
                        statusClass,
                        vm.getStatus(),
                        vm.getDiffPercentage() * 100,
                        vm.getTolerance() * 100
                ));
            }

            html.append("</tbody></table></div>");
        }

        // API metrics if present
        if (!metrics.getApiMetrics().isEmpty()) {
            html.append("""
                <div class="card">
                    <h2>API Call Metrics</h2>
                    <table>
                        <thead>
                            <tr>
                                <th>Test</th>
                                <th>Method</th>
                                <th>Endpoint</th>
                                <th>Status</th>
                                <th>Response Time</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

            for (TestMetrics.ApiMetric am : metrics.getApiMetrics()) {
                String statusClass = am.isSuccess() ? "status-passed" : "status-failed";
                html.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%s</td>
                        <td><span class="status %s">%d</span></td>
                        <td>%dms</td>
                    </tr>
                    """.formatted(
                        escapeHtml(am.getTestName()),
                        am.getMethod(),
                        escapeHtml(truncate(am.getEndpoint(), 50)),
                        statusClass,
                        am.getStatusCode(),
                        am.getResponseTimeMs()
                ));
            }

            html.append("</tbody></table></div>");
        }

        html.append("</div></body></html>");

        return html.toString();
    }

    @Override
    public String getFileExtension() {
        return "html";
    }

    private void addMetricBox(StringBuilder html, String label, String value, String colorClass) {
        html.append("""
            <div class="metric-box">
                <div class="metric-value %s">%s</div>
                <div class="metric-label">%s</div>
            </div>
            """.formatted(colorClass, escapeHtml(value), escapeHtml(label)));
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getSimpleClassName(String fullName) {
        if (fullName == null) return "";
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        if (millis < 60000) return String.format("%.1fs", millis / 1000.0);
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format("%dm %ds", minutes, seconds);
    }
}
