package ca.bnc.ciam.autotests.xray;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for Xray test execution import.
 * Supports the Xray JSON format for importing test execution results.
 */
@Data
@Builder
@Slf4j
@JsonInclude(JsonInclude.Include.NON_NULL)
public class XrayTestExecution {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /**
     * Test execution key (if updating existing).
     */
    private String testExecutionKey;

    /**
     * Test execution info for new executions.
     */
    private Info info;

    /**
     * List of test results.
     */
    @Builder.Default
    private List<TestResult> tests = new ArrayList<>();

    /**
     * Convert to JSON string for API import.
     */
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            log.error("Failed to serialize XrayTestExecution to JSON", e);
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Add a test result.
     */
    public XrayTestExecution addTest(TestResult test) {
        this.tests.add(test);
        return this;
    }

    /**
     * Test execution info.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Info {
        private String project;
        private String summary;
        private String description;
        private String version;
        private String revision;
        private String user;
        private String startDate;
        private String finishDate;
        private String testPlanKey;
        @Builder.Default
        private List<String> testEnvironments = new ArrayList<>();

        public static Info create(String project, String summary) {
            return Info.builder()
                    .project(project)
                    .summary(summary)
                    .startDate(LocalDateTime.now().format(DATE_FORMATTER))
                    .build();
        }
    }

    /**
     * Individual test result.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TestResult {
        /**
         * Test issue key in Jira.
         */
        private String testKey;

        /**
         * Test execution start time.
         */
        private String start;

        /**
         * Test execution finish time.
         */
        private String finish;

        /**
         * Test comment/notes.
         */
        private String comment;

        /**
         * Test status: PASSED, FAILED, TODO, EXECUTING, etc.
         */
        private String status;

        /**
         * Actual results description.
         */
        private String actualResult;

        /**
         * Test steps with individual results.
         */
        @Builder.Default
        private List<StepResult> steps = new ArrayList<>();

        /**
         * Iterations for data-driven tests.
         */
        @Builder.Default
        private List<Iteration> iterations = new ArrayList<>();

        /**
         * Evidence/attachments.
         */
        @Builder.Default
        private List<Evidence> evidences = new ArrayList<>();

        /**
         * Defects linked to this test.
         */
        @Builder.Default
        private List<String> defects = new ArrayList<>();

        /**
         * Add a step result.
         */
        public TestResult addStep(StepResult step) {
            this.steps.add(step);
            return this;
        }

        /**
         * Add an iteration.
         */
        public TestResult addIteration(Iteration iteration) {
            this.iterations.add(iteration);
            return this;
        }

        /**
         * Add evidence.
         */
        public TestResult addEvidence(Evidence evidence) {
            this.evidences.add(evidence);
            return this;
        }

        /**
         * Create a passing test result.
         */
        public static TestResult passed(String testKey) {
            return TestResult.builder()
                    .testKey(testKey)
                    .status("PASSED")
                    .start(LocalDateTime.now().format(DATE_FORMATTER))
                    .finish(LocalDateTime.now().format(DATE_FORMATTER))
                    .build();
        }

        /**
         * Create a failing test result.
         */
        public static TestResult failed(String testKey, String reason) {
            return TestResult.builder()
                    .testKey(testKey)
                    .status("FAILED")
                    .comment(reason)
                    .start(LocalDateTime.now().format(DATE_FORMATTER))
                    .finish(LocalDateTime.now().format(DATE_FORMATTER))
                    .build();
        }
    }

    /**
     * Test step result.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StepResult {
        /**
         * Step status: PASSED, FAILED, TODO, etc.
         */
        private String status;

        /**
         * Step comment.
         */
        private String comment;

        /**
         * Actual result for this step.
         */
        private String actualResult;

        /**
         * Evidence for this step.
         */
        @Builder.Default
        private List<Evidence> evidences = new ArrayList<>();

        /**
         * Defects for this step.
         */
        @Builder.Default
        private List<String> defects = new ArrayList<>();

        /**
         * Create a passing step.
         */
        public static StepResult passed() {
            return StepResult.builder().status("PASSED").build();
        }

        /**
         * Create a passing step with comment.
         */
        public static StepResult passed(String comment) {
            return StepResult.builder().status("PASSED").comment(comment).build();
        }

        /**
         * Create a failing step.
         */
        public static StepResult failed(String reason) {
            return StepResult.builder().status("FAILED").actualResult(reason).build();
        }
    }

    /**
     * Iteration for data-driven tests.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Iteration {
        /**
         * Iteration name/identifier.
         */
        private String name;

        /**
         * Iteration status.
         */
        private String status;

        /**
         * Parameters used in this iteration.
         */
        @Builder.Default
        private List<Parameter> parameters = new ArrayList<>();

        /**
         * Steps in this iteration.
         */
        @Builder.Default
        private List<StepResult> steps = new ArrayList<>();

        /**
         * Add parameter.
         */
        public Iteration addParameter(String name, String value) {
            this.parameters.add(Parameter.builder().name(name).value(value).build());
            return this;
        }

        /**
         * Add step result.
         */
        public Iteration addStep(StepResult step) {
            this.steps.add(step);
            return this;
        }
    }

    /**
     * Parameter for data-driven tests.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Parameter {
        private String name;
        private String value;
    }

    /**
     * Evidence/attachment.
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Evidence {
        /**
         * Base64 encoded data.
         */
        private String data;

        /**
         * Filename.
         */
        private String filename;

        /**
         * MIME content type.
         */
        private String contentType;

        /**
         * Create evidence from byte array.
         */
        public static Evidence fromBytes(String filename, byte[] data, String contentType) {
            return Evidence.builder()
                    .filename(filename)
                    .data(java.util.Base64.getEncoder().encodeToString(data))
                    .contentType(contentType)
                    .build();
        }

        /**
         * Create PNG screenshot evidence.
         */
        public static Evidence screenshot(String filename, byte[] screenshotData) {
            return fromBytes(filename, screenshotData, "image/png");
        }
    }
}
