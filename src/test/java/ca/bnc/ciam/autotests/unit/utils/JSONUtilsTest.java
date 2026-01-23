package ca.bnc.ciam.autotests.unit.utils;

import ca.bnc.ciam.autotests.utils.JSONUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for JSONUtils class.
 * Tests JSON parsing, serialization, and file operations.
 */
@Test(groups = "unit")
public class JSONUtilsTest {

    private Path tempDir;
    private File testJsonFile;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("jsonutils-test");
        testJsonFile = new File(tempDir.toFile(), "test.json");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (testJsonFile != null && testJsonFile.exists()) {
            testJsonFile.delete();
        }
        if (tempDir != null && tempDir.toFile().exists()) {
            tempDir.toFile().delete();
        }
    }

    // ===========================================
    // getObjectMapper Tests
    // ===========================================

    @Test
    public void testGetObjectMapper_ReturnsNonNull() {
        ObjectMapper mapper = JSONUtils.getObjectMapper();
        assertThat(mapper).isNotNull();
    }

    @Test
    public void testGetObjectMapper_ReturnsSameInstance() {
        ObjectMapper mapper1 = JSONUtils.getObjectMapper();
        ObjectMapper mapper2 = JSONUtils.getObjectMapper();
        assertThat(mapper1).isSameAs(mapper2);
    }

    // ===========================================
    // toJson Tests
    // ===========================================

    @Test
    public void testToJson_SimpleObject_ReturnsJson() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "test");

        String json = JSONUtils.toJson(map);
        assertThat(json).contains("\"name\"");
        assertThat(json).contains("\"test\"");
    }

    @Test
    public void testToJson_NullObject_ReturnsNullString() {
        String json = JSONUtils.toJson(null);
        assertThat(json).isEqualTo("null");
    }

    // ===========================================
    // toPrettyJson Tests
    // ===========================================

    @Test
    public void testToPrettyJson_SimpleObject_ReturnsFormattedJson() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "test");

        String json = JSONUtils.toPrettyJson(map);
        assertThat(json).contains("\"name\"");
        // Pretty printed should have newlines
        assertThat(json).contains("\n");
    }

    // ===========================================
    // fromJson Tests
    // ===========================================

    @Test
    public void testFromJson_ValidJson_ReturnsObject() {
        String json = "{\"name\":\"test\",\"value\":42}";
        TestObject result = JSONUtils.fromJson(json, TestObject.class);

        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("test");
        assertThat(result.value).isEqualTo(42);
    }

    @Test
    public void testFromJson_InvalidJson_ReturnsNull() {
        String json = "not valid json";
        TestObject result = JSONUtils.fromJson(json, TestObject.class);
        assertThat(result).isNull();
    }

    @Test
    public void testFromJson_NullJson_ThrowsException() {
        // Library doesn't handle null input gracefully - it throws IllegalArgumentException
        assertThatThrownBy(() -> JSONUtils.fromJson(null, TestObject.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===========================================
    // jsonToMap Tests
    // ===========================================

    @Test
    public void testJsonToMap_ValidJson_ReturnsMap() {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        Map<String, Object> result = JSONUtils.jsonToMap(json);

        assertThat(result).isNotNull();
        assertThat(result).containsEntry("key1", "value1");
        assertThat(result).containsEntry("key2", "value2");
    }

    @Test
    public void testJsonToMap_InvalidJson_ReturnsNull() {
        String json = "not valid json";
        Map<String, Object> result = JSONUtils.jsonToMap(json);
        assertThat(result).isNull();
    }

    // ===========================================
    // mapToJSONObject Tests
    // ===========================================

    @Test
    public void testMapToJSONObject_ValidMap_ReturnsJSONObject() {
        Map<String, String> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        JSONObject result = JSONUtils.mapToJSONObject(map);

        assertThat(result).isNotNull();
        assertThat(result.getString("key1")).isEqualTo("value1");
        assertThat(result.getString("key2")).isEqualTo("value2");
    }

    // ===========================================
    // isValidJson Tests
    // ===========================================

    @Test
    public void testIsValidJson_ValidObject_ReturnsTrue() {
        String json = "{\"name\":\"test\"}";
        boolean result = JSONUtils.isValidJson(json);
        assertThat(result).isTrue();
    }

    @Test
    public void testIsValidJson_ValidArray_ReturnsTrue() {
        String json = "[1, 2, 3]";
        boolean result = JSONUtils.isValidJson(json);
        assertThat(result).isTrue();
    }

    @Test
    public void testIsValidJson_InvalidJson_ReturnsFalse() {
        String json = "not valid json";
        boolean result = JSONUtils.isValidJson(json);
        assertThat(result).isFalse();
    }

    @Test
    public void testIsValidJson_EmptyString_ReturnsFalse() {
        boolean result = JSONUtils.isValidJson("");
        assertThat(result).isFalse();
    }

    @Test
    public void testIsValidJson_NullString_ThrowsException() {
        // Library doesn't handle null input gracefully - it throws NullPointerException
        assertThatThrownBy(() -> JSONUtils.isValidJson(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ===========================================
    // File Operations Tests
    // ===========================================

    @Test
    public void testReadJSONFromFile_ValidFile_ReturnsJSONObject() throws IOException {
        String content = "{\"name\":\"test\",\"value\":42}";
        Files.writeString(testJsonFile.toPath(), content);

        JSONObject result = JSONUtils.readJSONFromFile(testJsonFile);

        assertThat(result).isNotNull();
        assertThat(result.getString("name")).isEqualTo("test");
        assertThat(result.getInt("value")).isEqualTo(42);
    }

    @Test
    public void testReadJSONArrayFromFile_ValidFile_ReturnsJSONArray() throws IOException {
        String content = "[\"item1\", \"item2\", \"item3\"]";
        Files.writeString(testJsonFile.toPath(), content);

        JSONArray result = JSONUtils.readJSONArrayFromFile(testJsonFile, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(result).isNotNull();
        assertThat(result.length()).isEqualTo(3);
        assertThat(result.getString(0)).isEqualTo("item1");
    }

    @Test
    public void testWriteToFile_ValidObject_WritesFile() throws IOException {
        TestObject obj = new TestObject();
        obj.name = "test";
        obj.value = 42;

        JSONUtils.writeToFile(testJsonFile, obj);

        assertThat(testJsonFile.exists()).isTrue();
        String content = Files.readString(testJsonFile.toPath());
        assertThat(content).contains("\"name\"");
        assertThat(content).contains("\"test\"");
    }

    @Test
    public void testReadFromFile_ValidFile_ReturnsTypedObject() throws IOException {
        String content = "{\"name\":\"test\",\"value\":42}";
        Files.writeString(testJsonFile.toPath(), content);

        TestObject result = JSONUtils.readFromFile(testJsonFile, TestObject.class);

        assertThat(result).isNotNull();
        assertThat(result.name).isEqualTo("test");
        assertThat(result.value).isEqualTo(42);
    }

    @Test
    public void testReadFromFile_NonExistentFile_ThrowsException() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent.json");

        assertThatThrownBy(() -> JSONUtils.readFromFile(nonExistent, TestObject.class))
                .isInstanceOf(IOException.class);
    }

    // ===========================================
    // Test Helper Class
    // ===========================================

    public static class TestObject {
        public String name;
        public int value;

        public TestObject() {
        }
    }
}
