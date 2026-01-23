package ca.bnc.ciam.autotests.xray;

import ca.bnc.ciam.autotests.metrics.TestMetrics;

import java.time.format.DateTimeFormatter;

/**
 * Formats TestMetrics as Jira Wiki Markup for display in Jira issues.
 *
 * Jira Wiki Markup reference:
 * - h1. h2. h3. for headings
 * - ||header||header|| for table headers
 * - |cell|cell| for table rows
 * - {color:green}text{color} for colored text
 * - (/) for checkmark, (x) for X mark
 */
public final class JiraWikiMarkupFormatter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private JiraWikiMarkupFormatter() {
        // Utility class
    }

    /**
     * Format full TestMetrics as Jira Wiki Markup.
     *
     * @param metrics The metrics to format
     * @return Jira Wiki Markup formatted string
     */
    public static String format(TestMetrics metrics) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("h2. Test Execution Metrics\n\n");

        // Execution Summary
        appendExecutionSummary(sb, metrics);

        // Test Results Summary
        appendTestResultsSummary(sb, metrics);

        // Test Details Table
        appendTestDetailsTable(sb, metrics);

        // API Metrics (if any)
        if (metrics.getApiMetrics() != null && !metrics.getApiMetrics().isEmpty()) {
            appendApiMetrics(sb, metrics);
        }

        // Visual Metrics (if any)
        if (metrics.getVisualMetrics() != null && !metrics.getVisualMetrics().isEmpty()) {
            appendVisualMetrics(sb, metrics);
        }

        // Custom Metrics (if any)
        if (metrics.getCustomMetrics() != null && !metrics.getCustomMetrics().isEmpty()) {
            appendCustomMetrics(sb, metrics);
        }

        // Footer
        sb.append("\n----\n");
        sb.append("_Generated automatically by Test Automation Framework_\n");

        return sb.toString();
    }

    /**
     * Format a compact summary (for limited space).
     */
    public static String formatCompact(TestMetrics metrics) {
        StringBuilder sb = new StringBuilder();

        sb.append("h3. Test Run: ").append(metrics.getRunId()).append("\n\n");

        // Quick stats
        sb.append("||Metric||Value||\n");
        sb.append("|Pass Rate|").append(formatPassRate(metrics.getPassRate())).append("|\n");
        sb.append("|Total|").append(metrics.getTotalTests()).append("|\n");
        sb.append("|Passed|{color:green}").append(metrics.getPassedTests()).append("{color}|\n");
        sb.append("|Failed|{color:red}").append(metrics.getFailedTests()).append("{color}|\n");
        sb.append("|Skipped|{color:orange}").append(metrics.getSkippedTests()).append("{color}|\n");
        sb.append("|Duration|").append(formatDuration(metrics.getDurationMs())).append("|\n");
        sb.append("|Environment|").append(nullSafe(metrics.getEnvironment())).append("|\n");

        return sb.toString();
    }

    private static void appendExecutionSummary(StringBuilder sb, TestMetrics metrics) {
        sb.append("h3. Execution Summary\n\n");
        sb.append("||Property||Value||\n");
        sb.append("|Run ID|").append(nullSafe(metrics.getRunId())).append("|\n");
        sb.append("|Suite Name|").append(nullSafe(metrics.getSuiteName())).append("|\n");
        sb.append("|Start Time|").append(formatDateTime(metrics.getStartTime())).append("|\n");
        sb.append("|End Time|").append(formatDateTime(metrics.getEndTime())).append("|\n");
        sb.append("|Duration|").append(formatDuration(metrics.getDurationMs())).append("|\n");
        sb.append("|Environment|").append(nullSafe(metrics.getEnvironment())).append("|\n");
        sb.append("|Browser|").append(nullSafe(metrics.getBrowser())).append("|\n");
        sb.append("|Execution Mode|").append(nullSafe(metrics.getExecutionMode())).append("|\n");
        sb.append("\n");
    }

    private static void appendTestResultsSummary(StringBuilder sb, TestMetrics metrics) {
        sb.append("h3. Test Results\n\n");

        // Visual pass rate indicator
        String passRateColor = getPassRateColor(metrics.getPassRate());
        sb.append("{color:").append(passRateColor).append("}");
        sb.append("*Pass Rate: ").append(String.format("%.1f%%", metrics.getPassRate())).append("*");
        sb.append("{color}\n\n");

        sb.append("||Total||Passed||Failed||Skipped||\n");
        sb.append("|").append(metrics.getTotalTests());
        sb.append("|{color:green}").append(metrics.getPassedTests()).append("{color}");
        sb.append("|{color:red}").append(metrics.getFailedTests()).append("{color}");
        sb.append("|{color:orange}").append(metrics.getSkippedTests()).append("{color}|\n");
        sb.append("\n");
    }

    private static void appendTestDetailsTable(StringBuilder sb, TestMetrics metrics) {
        if (metrics.getTestResults() == null || metrics.getTestResults().isEmpty()) {
            return;
        }

        sb.append("h3. Test Details\n\n");
        sb.append("||Class||Method||Status||Duration||Error||\n");

        for (TestMetrics.TestResult result : metrics.getTestResults()) {
            String statusIcon = getStatusIcon(result.getStatus());
            String statusColor = getStatusColor(result.getStatus());

            sb.append("|").append(getSimpleClassName(result.getClassName()));
            sb.append("|").append(nullSafe(result.getMethodName()));
            sb.append("|{color:").append(statusColor).append("}").append(statusIcon).append(" ").append(result.getStatus()).append("{color}");
            sb.append("|").append(formatDuration(result.getDurationMs()));
            sb.append("|").append(truncate(escapeWikiMarkup(result.getErrorMessage()), 50));
            sb.append("|\n");
        }
        sb.append("\n");
    }

    private static void appendApiMetrics(StringBuilder sb, TestMetrics metrics) {
        sb.append("h3. API Metrics\n\n");
        sb.append("||Test||Method||Endpoint||Status||Response Time||\n");

        for (TestMetrics.ApiMetric api : metrics.getApiMetrics()) {
            String statusColor = api.isSuccess() ? "green" : "red";

            sb.append("|").append(nullSafe(api.getTestName()));
            sb.append("|").append(nullSafe(api.getMethod()));
            sb.append("|").append(truncate(nullSafe(api.getEndpoint()), 40));
            sb.append("|{color:").append(statusColor).append("}").append(api.getStatusCode()).append("{color}");
            sb.append("|").append(api.getResponseTimeMs()).append("ms");
            sb.append("|\n");
        }
        sb.append("\n");
    }

    private static void appendVisualMetrics(StringBuilder sb, TestMetrics metrics) {
        sb.append("h3. Visual Comparison Results\n\n");
        sb.append("||Test||Baseline||Matched||Diff %||Status||\n");

        for (TestMetrics.VisualMetric visual : metrics.getVisualMetrics()) {
            String matchIcon = visual.isMatched() ? "(/) " : "(x) ";
            String matchColor = visual.isMatched() ? "green" : "red";

            sb.append("|").append(nullSafe(visual.getTestName()));
            sb.append("|").append(nullSafe(visual.getBaselineName()));
            sb.append("|{color:").append(matchColor).append("}").append(matchIcon).append(visual.isMatched() ? "Yes" : "No").append("{color}");
            sb.append("|").append(String.format("%.4f%%", visual.getDiffPercentage() * 100));
            sb.append("|").append(nullSafe(visual.getStatus()));
            sb.append("|\n");
        }
        sb.append("\n");
    }

    private static void appendCustomMetrics(StringBuilder sb, TestMetrics metrics) {
        sb.append("h3. Custom Metrics\n\n");
        sb.append("||Key||Value||\n");

        for (var entry : metrics.getCustomMetrics().entrySet()) {
            sb.append("|").append(entry.getKey());
            sb.append("|").append(entry.getValue());
            sb.append("|\n");
        }
        sb.append("\n");
    }

    // Helper methods

    private static String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return dateTime.format(DATE_FORMAT);
    }

    private static String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        if (millis < 60000) {
            return String.format("%.1fs", millis / 1000.0);
        }
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format("%dm %ds", minutes, seconds);
    }

    private static String formatPassRate(double passRate) {
        String color = getPassRateColor(passRate);
        return "{color:" + color + "}" + String.format("%.1f%%", passRate) + "{color}";
    }

    private static String getPassRateColor(double passRate) {
        if (passRate >= 90) return "green";
        if (passRate >= 70) return "orange";
        return "red";
    }

    private static String getStatusIcon(TestMetrics.TestResult.Status status) {
        return switch (status) {
            case PASSED -> "(/)";
            case FAILED -> "(x)";
            case SKIPPED -> "(!)";
        };
    }

    private static String getStatusColor(TestMetrics.TestResult.Status status) {
        return switch (status) {
            case PASSED -> "green";
            case FAILED -> "red";
            case SKIPPED -> "orange";
        };
    }

    private static String getSimpleClassName(String fullName) {
        if (fullName == null) return "";
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private static String escapeWikiMarkup(String text) {
        if (text == null) return "";
        // Escape special wiki markup characters
        return text
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("|", "\\|")
                .replace("\n", " ")
                .replace("\r", "");
    }
}
