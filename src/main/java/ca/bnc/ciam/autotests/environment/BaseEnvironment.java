package ca.bnc.ciam.autotests.environment;

import ca.bnc.ciam.autotests.base.AbstractDataDrivenTest;
import ca.bnc.ciam.autotests.utils.JSONUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base environment class for loading test data from JSON files.
 * Supports environment-specific data loading with fallback to debug configuration.
 *
 * <p>Automatically detects execution environment (CI/CD pipeline vs local) using
 * environment variables set by common CI systems.</p>
 */
@Slf4j
public class BaseEnvironment extends AbstractDataDrivenTest {

    /**
     * Default path to debug configuration file.
     */
    public static final String CONFIG_FILE_PATH = "src/test/resources/debug_config.json";

    /**
     * System property name for data manager path.
     */
    public static final String DATA_MANAGER_PROPERTY = "bnc.data.manager";

    /**
     * System property to force local execution mode (overrides auto-detection).
     */
    public static final String FORCE_LOCAL_PROPERTY = "bnc.force.local";

    /**
     * System property to force pipeline execution mode (overrides auto-detection).
     */
    public static final String FORCE_PIPELINE_PROPERTY = "bnc.force.pipeline";

    private static String absolutePathToDataManager;

    // =========================================================================
    // Pipeline/Local Execution Detection
    // =========================================================================

    /**
     * Detects if the tests are running in a CI/CD pipeline environment.
     *
     * <p>Checks for common CI/CD environment variables:</p>
     * <ul>
     *   <li>CI - Generic CI flag (set by most CI systems)</li>
     *   <li>JENKINS_HOME - Jenkins</li>
     *   <li>GITLAB_CI - GitLab CI</li>
     *   <li>GITHUB_ACTIONS - GitHub Actions</li>
     *   <li>TF_BUILD - Azure DevOps Pipelines</li>
     *   <li>BITBUCKET_BUILD_NUMBER - Bitbucket Pipelines</li>
     *   <li>CIRCLECI - CircleCI</li>
     *   <li>TRAVIS - Travis CI</li>
     *   <li>BUILD_NUMBER - Jenkins and many other CI systems</li>
     * </ul>
     *
     * <p>Can be overridden using system properties:</p>
     * <ul>
     *   <li>-Dbnc.force.local=true - Force local execution</li>
     *   <li>-Dbnc.force.pipeline=true - Force pipeline execution</li>
     * </ul>
     *
     * @return true if running in a CI/CD pipeline, false for local execution
     */
    public static boolean isRunningInPipeline() {
        // Check for force overrides first
        if ("true".equalsIgnoreCase(System.getProperty(FORCE_LOCAL_PROPERTY))) {
            log.debug("Forced local execution via system property");
            return false;
        }
        if ("true".equalsIgnoreCase(System.getProperty(FORCE_PIPELINE_PROPERTY))) {
            log.debug("Forced pipeline execution via system property");
            return true;
        }

        // Auto-detect based on CI environment variables
        boolean inPipeline = isEnvSet("CI") ||
                isEnvSet("JENKINS_HOME") ||
                isEnvSet("GITLAB_CI") ||
                isEnvSet("GITHUB_ACTIONS") ||
                isEnvSet("TF_BUILD") ||
                isEnvSet("BITBUCKET_BUILD_NUMBER") ||
                isEnvSet("CIRCLECI") ||
                isEnvSet("TRAVIS") ||
                isEnvSet("BUILD_NUMBER") ||
                isEnvSet("TEAMCITY_VERSION");

        if (inPipeline) {
            log.info("Detected CI/CD pipeline execution environment");
        } else {
            log.info("Detected local execution environment");
        }

        return inPipeline;
    }

    /**
     * Detects if the tests are running locally (not in a CI/CD pipeline).
     *
     * @return true if running locally, false if in a CI/CD pipeline
     */
    public static boolean isRunningLocally() {
        return !isRunningInPipeline();
    }

    /**
     * Gets the detected CI system name, if any.
     *
     * @return The CI system name (e.g., "Jenkins", "GitLab CI", "GitHub Actions")
     *         or "Local" if not in a pipeline, or "Unknown CI" if in a pipeline but system not identified
     */
    public static String getDetectedCISystem() {
        if ("true".equalsIgnoreCase(System.getProperty(FORCE_LOCAL_PROPERTY))) {
            return "Local (forced)";
        }
        if ("true".equalsIgnoreCase(System.getProperty(FORCE_PIPELINE_PROPERTY))) {
            return "Pipeline (forced)";
        }

        if (isEnvSet("JENKINS_HOME") || isEnvSet("JENKINS_URL")) {
            return "Jenkins";
        }
        if (isEnvSet("GITLAB_CI")) {
            return "GitLab CI";
        }
        if (isEnvSet("GITHUB_ACTIONS")) {
            return "GitHub Actions";
        }
        if (isEnvSet("TF_BUILD")) {
            return "Azure DevOps";
        }
        if (isEnvSet("BITBUCKET_BUILD_NUMBER")) {
            return "Bitbucket Pipelines";
        }
        if (isEnvSet("CIRCLECI")) {
            return "CircleCI";
        }
        if (isEnvSet("TRAVIS")) {
            return "Travis CI";
        }
        if (isEnvSet("TEAMCITY_VERSION")) {
            return "TeamCity";
        }
        if (isEnvSet("CI") || isEnvSet("BUILD_NUMBER")) {
            return "Unknown CI";
        }

        return "Local";
    }

    /**
     * Checks if an environment variable is set and non-empty.
     *
     * @param varName The environment variable name
     * @return true if set and non-empty
     */
    private static boolean isEnvSet(String varName) {
        String value = System.getenv(varName);
        return value != null && !value.trim().isEmpty();
    }

    // =========================================================================
    // Test Data Building
    // =========================================================================

    /**
     * Build test environment data for the given test ID.
     *
     * @param testId The test ID to load data for
     * @return Iterator of test data arrays
     */
    public static Iterator<Object[]> buildTestEnvironment(String testId) {
        return buildTestData(testId);
    }

    /**
     * Build test environment data as a Collection for the given test ID.
     * This is a convenience method that returns a Collection instead of Iterator,
     * making it easier to use with TestNG @DataProvider and other Collection-based APIs.
     *
     * @param testId The test ID to load data for
     * @return Collection of test data arrays
     */
    public static Collection<Object[]> buildTestEnvironmentAsCollection(String testId) {
        Iterator<Object[]> iter = buildTestEnvironment(testId);
        List<Object[]> list = new ArrayList<>();
        iter.forEachRemaining(list::add);
        return list;
    }

    /**
     * Build test data from JSON for the given test ID.
     *
     * @param testId The test ID
     * @return Iterator of test data arrays
     */
    protected static Iterator<Object[]> buildTestData(String testId) {
        List<Map<String, String>> data = buildTestDataFromJson(testId);
        return data.stream().map(map -> new Object[]{map}).iterator();
    }

    /**
     * Build test data list from JSON files.
     *
     * @param testId The test ID
     * @return List of test data maps
     */
    private static List<Map<String, String>> buildTestDataFromJson(String testId) {
        JSONObject dataManagerJson = getDataManagerAsJSON();

        if (dataManagerJson == null || !dataManagerJson.has(testId)) {
            log.warn("Test ID = [{}] cannot be found in Data Manager {}. Returning empty data.",
                    testId, absolutePathToDataManager);
            return Collections.emptyList();
        }

        JSONArray dataAssignedToTest = dataManagerJson.optJSONArray(testId);
        if (dataAssignedToTest == null) {
            log.warn("Structural error: It must be an array entry like [...] for the test ID = [{}] in Data Manager {}. Data not built.",
                    testId, absolutePathToDataManager);
            return Collections.emptyList();
        }

        List<Map<String, String>> dataListOfMaps = new ArrayList<>(dataAssignedToTest.length());
        log.info("Getting JSON test data for [{}]", testId);

        for (int i = 0; i < dataAssignedToTest.length(); ++i) {
            log.info("Data set #{}", i);
            JSONObject singleIterationData = dataAssignedToTest.optJSONObject(i);

            if (singleIterationData == null) {
                log.warn("Structural error: It must be an object entry like {...} for the iteration #{} and the test ID = [{}] in Data Manager {}. Data not built.",
                        i, testId, absolutePathToDataManager);
                return Collections.emptyList();
            }

            Map<String, String> dataForSingleIteration = buildDataForSingleIteration(testId, i, singleIterationData);
            dataListOfMaps.add(dataForSingleIteration);
        }

        return dataListOfMaps;
    }

    /**
     * Build data map for a single test iteration.
     */
    private static Map<String, String> buildDataForSingleIteration(String testId, int iteration, JSONObject singleIterationData) {
        Map<String, String> retVal = new HashMap<>();

        for (String fileKey : singleIterationData.keySet()) {
            if ("descriptor".equalsIgnoreCase(fileKey) || "comment".equalsIgnoreCase(fileKey)) {
                retVal.put(fileKey, singleIterationData.getString(fileKey));
                log.info("Data {}: {}", fileKey, singleIterationData.getString(fileKey));
            } else {
                JSONArray idsJson = singleIterationData.optJSONArray(fileKey);
                if (idsJson != null) {
                    List<String> ids = idsJson.toList().stream()
                            .map(Object::toString)
                            .toList();
                    appendDataFromFile(fileKey, ids, retVal);
                } else {
                    log.warn("Something wrong for the testId = [{}] in the data set #{} for the key {}",
                            testId, iteration, fileKey);
                }
            }
        }

        return retVal;
    }

    /**
     * Append data from a specific JSON file.
     */
    private static void appendDataFromFile(String fileName, List<String> ids, Map<String, String> dataRecipient) {
        String dataFolder = new File(absolutePathToDataManager).getParent();
        if (dataFolder == null) {
            log.error("Invalid absolute path to data manager: {}", absolutePathToDataManager);
            return;
        }

        log.info("Read from data file {}", fileName);

        File dataFile = new File(dataFolder, fileName);

        try {
            // Canonicalize the paths to prevent path traversal
            String canonicalDataFolder = new File(dataFolder).getCanonicalPath();
            String canonicalDataFile = dataFile.getCanonicalPath();

            // Ensure that the file is within the expected directory
            if (!canonicalDataFile.startsWith(canonicalDataFolder)) {
                log.error("Potential path traversal attempt: {}", fileName);
                return;
            }

            if (!dataFile.exists()) {
                log.error("Data file does not exist at path: {}", dataFile.getAbsolutePath());
                return;
            }

            JSONObject dataFromFile = JSONUtils.readJSONFromFile(dataFile, StandardCharsets.UTF_8);

            for (int j = 0; j < ids.size(); ++j) {
                String id = ids.get(j);
                JSONObject dataForTest = dataFromFile.optJSONObject(id);
                if (dataForTest == null) {
                    log.warn("Data not found for ID '{}' in file '{}'", id, fileName);
                    continue;
                }

                for (String entryKey : dataForTest.keySet()) {
                    String keyToAdd = String.format("%s:%s:%d", fileName, entryKey, j + 1);
                    String valueToAdd = getValueFromJSONObject(dataForTest, entryKey);
                    dataRecipient.put(keyToAdd, valueToAdd);
                }
            }
        } catch (IOException | JSONException e) {
            log.error("Error processing data file: {} {}", dataFile.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Get a value from a JSON object, handling both simple values and JSON structures.
     */
    private static String getValueFromJSONObject(JSONObject jsonObject, String key) {
        String value;
        if (!key.startsWith("as-json-")) {
            value = jsonObject.optString(key);
        } else {
            try {
                value = jsonObject.getJSONObject(key).toString();
            } catch (JSONException e) {
                value = jsonObject.getJSONArray(key).toString();
            }
        }
        return value;
    }

    /**
     * Get the data manager JSON object.
     */
    private static JSONObject getDataManagerAsJSON() {
        String pathToData = validateProperty();
        if (pathToData == null) {
            return null;
        }

        return readDataManagerFromFile(pathToData);
    }

    /**
     * Read the data manager JSON from file.
     */
    private static JSONObject readDataManagerFromFile(String pathToData) {
        File dataManagerFile = new File(pathToData);

        try {
            // Canonicalize the path to prevent path traversal
            if (!dataManagerFile.isAbsolute()) {
                log.info("Using relative path for Data Manager file: {}", pathToData);
                String resourcePath = Objects.requireNonNull(
                        BaseEnvironment.class.getClassLoader().getResource("")
                ).getPath();

                // Handle Windows path (remove leading slash if present)
                if (resourcePath.startsWith("/") && resourcePath.contains(":")) {
                    resourcePath = resourcePath.substring(1);
                }

                String fullPath = resourcePath + pathToData;

                // Safeguard against path traversal by sanitizing only the file name
                String sanitizedFileName = FilenameUtils.getName(fullPath);
                dataManagerFile = new File(FilenameUtils.getFullPathNoEndSeparator(fullPath), sanitizedFileName);
            }

            // Get canonical paths to ensure the file path is valid
            String canonicalPath = dataManagerFile.getCanonicalPath();

            // Check if the data manager file exists
            if (!dataManagerFile.exists()) {
                log.error("Data Manager file does not exist at path: {}", canonicalPath);
                return null;
            }

            // Store the absolute path of the data manager file
            absolutePathToDataManager = canonicalPath;
            log.info("Data manager file absolute path is {}", absolutePathToDataManager);

            // Read JSON from the file
            return JSONUtils.readJSONFromFile(dataManagerFile, StandardCharsets.UTF_8);

        } catch (IOException | JSONException e) {
            log.error("Error processing data manager file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate and get the data manager path from system property or config file.
     */
    private static String validateProperty() {
        String pathToData = System.getProperty(DATA_MANAGER_PROPERTY);

        // Read path from config if system property is not set (local/debug execution)
        if (pathToData == null) {
            pathToData = readPathToDataFromConfig();
        }

        // Check if pathToData is still null after reading from config
        if (pathToData == null) {
            log.error("Path to data manager is null. Set system property '{}' or configure '{}'",
                    DATA_MANAGER_PROPERTY, CONFIG_FILE_PATH);
            return null;
        }

        return pathToData;
    }

    /**
     * Read the data manager path from the debug configuration file.
     */
    private static String readPathToDataFromConfig() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File configFile = new File(CONFIG_FILE_PATH);
            if (!configFile.exists()) {
                log.warn("Debug config file not found at: {}", CONFIG_FILE_PATH);
                return null;
            }
            Map<String, Object> configData = objectMapper.readValue(configFile, new TypeReference<>() {});
            return (String) configData.get("data");
        } catch (IOException e) {
            log.error("Error reading the configuration file at {}: {}", CONFIG_FILE_PATH, e.getMessage());
            return null;
        }
    }

    /**
     * Get the current environment name from the data manager path.
     *
     * @return The environment name (e.g., "development-ti", "staging-ta")
     */
    public static String getCurrentEnvironment() {
        if (absolutePathToDataManager == null) {
            return "unknown";
        }
        File dataManagerFile = new File(absolutePathToDataManager);
        File parentDir = dataManagerFile.getParentFile();
        return parentDir != null ? parentDir.getName() : "unknown";
    }

    /**
     * Get the absolute path to the current data manager file.
     *
     * @return The absolute path
     */
    public static String getDataManagerPath() {
        return absolutePathToDataManager;
    }
}
