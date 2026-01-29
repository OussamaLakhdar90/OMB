package ca.bnc.ciam.autotests.metrics.export;

import ca.bnc.ciam.autotests.metrics.TestMetrics;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

/**
 * Exports metrics to XML format.
 */
@Slf4j
public class XmlMetricsExporter implements MetricsExporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String INDENT = "  ";

    @Override
    public void export(TestMetrics metrics, Path outputPath) throws IOException {
        Files.createDirectories(outputPath.getParent());
        String xml = exportToString(metrics);
        Files.writeString(outputPath, xml);
        log.info("Metrics exported to XML: {}", outputPath);
    }

    @Override
    public String exportToString(TestMetrics metrics) {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<testReport>\n");

        // Summary section
        appendSummary(sb, metrics);

        // Test results section
        appendTestResults(sb, metrics);

        // Visual metrics section
        appendVisualMetrics(sb, metrics);

        // API metrics section
        appendApiMetrics(sb, metrics);

        sb.append("</testReport>\n");

        return sb.toString();
    }

    @Override
    public String getFileExtension() {
        return "xml";
    }

    private void appendSummary(StringBuilder sb, TestMetrics metrics) {
        sb.append(INDENT).append("<summary>\n");
        sb.append(INDENT).append(INDENT).append("<runId>").append(escapeXml(metrics.getRunId())).append("</runId>\n");
        sb.append(INDENT).append(INDENT).append("<suiteName>").append(escapeXml(metrics.getSuiteName())).append("</suiteName>\n");
        sb.append(INDENT).append(INDENT).append("<startTime>")
                .append(metrics.getStartTime() != null ? metrics.getStartTime().format(DATE_FORMAT) : "")
                .append("</startTime>\n");
        sb.append(INDENT).append(INDENT).append("<endTime>")
                .append(metrics.getEndTime() != null ? metrics.getEndTime().format(DATE_FORMAT) : "")
                .append("</endTime>\n");
        sb.append(INDENT).append(INDENT).append("<durationMs>").append(metrics.getDurationMs()).append("</durationMs>\n");
        sb.append(INDENT).append(INDENT).append("<environment>").append(escapeXml(metrics.getEnvironment())).append("</environment>\n");
        sb.append(INDENT).append(INDENT).append("<browser>").append(escapeXml(metrics.getBrowser())).append("</browser>\n");
        sb.append(INDENT).append(INDENT).append("<executionMode>").append(escapeXml(metrics.getExecutionMode())).append("</executionMode>\n");
        sb.append(INDENT).append(INDENT).append("<totalTests>").append(metrics.getTotalTests()).append("</totalTests>\n");
        sb.append(INDENT).append(INDENT).append("<passedTests>").append(metrics.getPassedTests()).append("</passedTests>\n");
        sb.append(INDENT).append(INDENT).append("<failedTests>").append(metrics.getFailedTests()).append("</failedTests>\n");
        sb.append(INDENT).append(INDENT).append("<skippedTests>").append(metrics.getSkippedTests()).append("</skippedTests>\n");
        sb.append(INDENT).append(INDENT).append("<passRate>").append(String.format("%.2f", metrics.getPassRate())).append("</passRate>\n");
        sb.append(INDENT).append("</summary>\n");
    }

    private void appendTestResults(StringBuilder sb, TestMetrics metrics) {
        sb.append(INDENT).append("<testResults>\n");

        for (TestMetrics.TestResult result : metrics.getTestResults()) {
            sb.append(INDENT).append(INDENT).append("<testResult>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<className>").append(escapeXml(result.getClassName())).append("</className>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<methodName>").append(escapeXml(result.getMethodName())).append("</methodName>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<status>").append(result.getStatus()).append("</status>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<startTime>")
                    .append(result.getStartTime() != null ? result.getStartTime().format(DATE_FORMAT) : "")
                    .append("</startTime>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<endTime>")
                    .append(result.getEndTime() != null ? result.getEndTime().format(DATE_FORMAT) : "")
                    .append("</endTime>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<durationMs>").append(result.getDurationMs()).append("</durationMs>\n");

            if (result.getErrorMessage() != null) {
                sb.append(INDENT).append(INDENT).append(INDENT).append("<errorMessage>")
                        .append(escapeXml(result.getErrorMessage())).append("</errorMessage>\n");
            }

            if (result.getStackTrace() != null) {
                sb.append(INDENT).append(INDENT).append(INDENT).append("<stackTrace><![CDATA[")
                        .append(result.getStackTrace()).append("]]></stackTrace>\n");
            }

            // Steps
            if (!result.getSteps().isEmpty()) {
                sb.append(INDENT).append(INDENT).append(INDENT).append("<steps>\n");
                for (TestMetrics.StepResult step : result.getSteps()) {
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("<step>\n");
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append(INDENT)
                            .append("<name>").append(escapeXml(step.getName())).append("</name>\n");
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append(INDENT)
                            .append("<status>").append(escapeXml(step.getStatus())).append("</status>\n");
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append(INDENT)
                            .append("<durationMs>").append(step.getDurationMs()).append("</durationMs>\n");
                    if (step.isHasScreenshot()) {
                        sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append(INDENT)
                                .append("<screenshotPath>").append(escapeXml(step.getScreenshotPath())).append("</screenshotPath>\n");
                    }
                    sb.append(INDENT).append(INDENT).append(INDENT).append(INDENT).append("</step>\n");
                }
                sb.append(INDENT).append(INDENT).append(INDENT).append("</steps>\n");
            }

            sb.append(INDENT).append(INDENT).append("</testResult>\n");
        }

        sb.append(INDENT).append("</testResults>\n");
    }

    private void appendVisualMetrics(StringBuilder sb, TestMetrics metrics) {
        if (metrics.getVisualMetrics().isEmpty()) {
            return;
        }

        sb.append(INDENT).append("<visualMetrics>\n");

        for (TestMetrics.VisualMetric vm : metrics.getVisualMetrics()) {
            sb.append(INDENT).append(INDENT).append("<visualMetric>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<testName>").append(escapeXml(vm.getTestName())).append("</testName>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<baselineName>").append(escapeXml(vm.getBaselineName())).append("</baselineName>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<matched>").append(vm.isMatched()).append("</matched>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<diffPercentage>")
                    .append(String.format("%.4f", vm.getDiffPercentage() * 100)).append("</diffPercentage>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<tolerance>")
                    .append(String.format("%.4f", vm.getTolerance() * 100)).append("</tolerance>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<status>").append(escapeXml(vm.getStatus())).append("</status>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<comparisonTimeMs>").append(vm.getComparisonTimeMs()).append("</comparisonTimeMs>\n");

            if (vm.getDiffImagePath() != null) {
                sb.append(INDENT).append(INDENT).append(INDENT).append("<diffImagePath>")
                        .append(escapeXml(vm.getDiffImagePath())).append("</diffImagePath>\n");
            }
            if (vm.getActualImagePath() != null) {
                sb.append(INDENT).append(INDENT).append(INDENT).append("<actualImagePath>")
                        .append(escapeXml(vm.getActualImagePath())).append("</actualImagePath>\n");
            }

            sb.append(INDENT).append(INDENT).append("</visualMetric>\n");
        }

        sb.append(INDENT).append("</visualMetrics>\n");
    }

    private void appendApiMetrics(StringBuilder sb, TestMetrics metrics) {
        if (metrics.getApiMetrics().isEmpty()) {
            return;
        }

        sb.append(INDENT).append("<apiMetrics>\n");

        for (TestMetrics.ApiMetric am : metrics.getApiMetrics()) {
            sb.append(INDENT).append(INDENT).append("<apiMetric>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<testName>").append(escapeXml(am.getTestName())).append("</testName>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<method>").append(escapeXml(am.getMethod())).append("</method>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<endpoint>").append(escapeXml(am.getEndpoint())).append("</endpoint>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<statusCode>").append(am.getStatusCode()).append("</statusCode>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<responseTimeMs>").append(am.getResponseTimeMs()).append("</responseTimeMs>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<requestSizeBytes>").append(am.getRequestSizeBytes()).append("</requestSizeBytes>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<responseSizeBytes>").append(am.getResponseSizeBytes()).append("</responseSizeBytes>\n");
            sb.append(INDENT).append(INDENT).append(INDENT).append("<success>").append(am.isSuccess()).append("</success>\n");
            sb.append(INDENT).append(INDENT).append("</apiMetric>\n");
        }

        sb.append(INDENT).append("</apiMetrics>\n");
    }

    /**
     * Escape XML special characters.
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
