package ca.bnc.ciam.autotests.unit.data;

import ca.bnc.ciam.autotests.data.TestData;
import ca.bnc.ciam.autotests.exception.TestDataException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TestData class.
 * Tests fluent API for accessing test data from maps.
 */
@Test(groups = "unit")
public class TestDataTest {

    private TestData testData;
    private Map<String, String> dataMap;

    @BeforeMethod
    public void setUp() {
        dataMap = new HashMap<>();
        dataMap.put("descriptor", "Test Description");
        dataMap.put("comment", "Test Comment");
        dataMap.put("users.json:username:1", "testuser");
        dataMap.put("users.json:password:1", "secret123");
        dataMap.put("users.json:age:1", "25");
        dataMap.put("users.json:active:1", "true");
        dataMap.put("users.json:score:1", "98.5");
        dataMap.put("users.json:count:1", "1000000");
        dataMap.put("users.json:as-json-profile:1", "{\"name\":\"Test\",\"level\":5}");
        dataMap.put("users.json:as-json-tags:1", "[\"tag1\",\"tag2\"]");
        dataMap.put("users.json:invalid:1", "not a number");

        testData = new TestData(dataMap);
    }

    // ===========================================
    // Constructor Tests
    // ===========================================

    @Test
    public void testDefaultConstructor_CreatesEmptyTestData() {
        TestData emptyData = new TestData();
        assertThat(emptyData).isNotNull();
    }

    @Test
    public void testConstructorWithMap_SetsDataMap() {
        TestData data = new TestData(dataMap);
        assertThat(data.getDataMap()).isEqualTo(dataMap);
    }

    // ===========================================
    // withData Tests
    // ===========================================

    @Test
    public void testWithData_SetsDataMap() {
        TestData data = new TestData().withData(dataMap);
        assertThat(data.getDataMap()).isEqualTo(dataMap);
    }

    @Test
    public void testWithData_ReturnsSameInstance() {
        TestData data = new TestData();
        TestData result = data.withData(dataMap);
        assertThat(result).isSameAs(data);
    }

    // ===========================================
    // from Tests
    // ===========================================

    @Test
    public void testFrom_SetsSourceFile() {
        testData.from("users.json");
        String value = testData.getForKey("username");
        assertThat(value).isEqualTo("testuser");
    }

    @Test
    public void testFrom_ReturnsSameInstance() {
        TestData result = testData.from("users.json");
        assertThat(result).isSameAs(testData);
    }

    // ===========================================
    // forIndex Tests
    // ===========================================

    @Test
    public void testForIndex_SetsIndex() {
        testData.from("users.json").forIndex(1);
        String value = testData.getForKey("username");
        assertThat(value).isEqualTo("testuser");
    }

    @Test
    public void testForIndex_ReturnsSameInstance() {
        TestData result = testData.forIndex(2);
        assertThat(result).isSameAs(testData);
    }

    // ===========================================
    // getForKey Tests
    // ===========================================

    @Test
    public void testGetForKey_ExistingKey_ReturnsValue() {
        String value = testData.from("users.json").forIndex(1).getForKey("username");
        assertThat(value).isEqualTo("testuser");
    }

    @Test
    public void testGetForKey_NonExistingKey_ReturnsNull() {
        String value = testData.from("users.json").forIndex(1).getForKey("nonexistent");
        assertThat(value).isNull();
    }

    @Test
    public void testGetForKey_WithDefault_ReturnsDefault() {
        String value = testData.from("users.json").forIndex(1).getForKey("nonexistent", "default");
        assertThat(value).isEqualTo("default");
    }

    @Test
    public void testGetForKey_WithDefault_ExistingKey_ReturnsValue() {
        String value = testData.from("users.json").forIndex(1).getForKey("username", "default");
        assertThat(value).isEqualTo("testuser");
    }

    // ===========================================
    // getBoolean Tests
    // ===========================================

    @Test
    public void testGetBoolean_TrueValue_ReturnsTrue() {
        Boolean value = testData.from("users.json").forIndex(1).getBoolean("active");
        assertThat(value).isTrue();
    }

    @Test
    public void testGetBoolean_NonExistingKey_ReturnsNull() {
        Boolean value = testData.from("users.json").forIndex(1).getBoolean("nonexistent");
        assertThat(value).isNull();
    }

    @Test
    public void testGetBoolean_WithDefault_NonExistingKey_ReturnsDefault() {
        boolean value = testData.from("users.json").forIndex(1).getBoolean("nonexistent", false);
        assertThat(value).isFalse();
    }

    // ===========================================
    // getInteger Tests
    // ===========================================

    @Test
    public void testGetInteger_ValidInt_ReturnsInteger() {
        Integer value = testData.from("users.json").forIndex(1).getInteger("age");
        assertThat(value).isEqualTo(25);
    }

    @Test
    public void testGetInteger_NonExistingKey_ReturnsNull() {
        Integer value = testData.from("users.json").forIndex(1).getInteger("nonexistent");
        assertThat(value).isNull();
    }

    @Test
    public void testGetInteger_InvalidFormat_ThrowsException() {
        assertThatThrownBy(() -> testData.from("users.json").forIndex(1).getInteger("invalid"))
                .isInstanceOf(TestDataException.class);
    }

    @Test
    public void testGetInteger_WithDefault_ReturnsDefault() {
        int value = testData.from("users.json").forIndex(1).getInteger("nonexistent", 99);
        assertThat(value).isEqualTo(99);
    }

    // ===========================================
    // getLong Tests
    // ===========================================

    @Test
    public void testGetLong_ValidLong_ReturnsLong() {
        Long value = testData.from("users.json").forIndex(1).getLong("count");
        assertThat(value).isEqualTo(1000000L);
    }

    @Test
    public void testGetLong_NonExistingKey_ReturnsNull() {
        Long value = testData.from("users.json").forIndex(1).getLong("nonexistent");
        assertThat(value).isNull();
    }

    @Test
    public void testGetLong_InvalidFormat_ThrowsException() {
        assertThatThrownBy(() -> testData.from("users.json").forIndex(1).getLong("invalid"))
                .isInstanceOf(TestDataException.class);
    }

    // ===========================================
    // getDouble Tests
    // ===========================================

    @Test
    public void testGetDouble_ValidDouble_ReturnsDouble() {
        Double value = testData.from("users.json").forIndex(1).getDouble("score");
        assertThat(value).isEqualTo(98.5);
    }

    @Test
    public void testGetDouble_NonExistingKey_ReturnsNull() {
        Double value = testData.from("users.json").forIndex(1).getDouble("nonexistent");
        assertThat(value).isNull();
    }

    @Test
    public void testGetDouble_InvalidFormat_ThrowsException() {
        assertThatThrownBy(() -> testData.from("users.json").forIndex(1).getDouble("invalid"))
                .isInstanceOf(TestDataException.class);
    }

    // ===========================================
    // getJSONObject Tests
    // ===========================================

    @Test
    public void testGetJSONObject_ValidJSON_ReturnsJSONObject() {
        var json = testData.from("users.json").forIndex(1).getJSONObject("profile");
        assertThat(json).isNotNull();
        assertThat(json.getString("name")).isEqualTo("Test");
        assertThat(json.getInt("level")).isEqualTo(5);
    }

    @Test
    public void testGetJSONObject_NonExistingKey_ReturnsNull() {
        var json = testData.from("users.json").forIndex(1).getJSONObject("nonexistent");
        assertThat(json).isNull();
    }

    @Test
    public void testGetJSONObject_InvalidJSON_ThrowsException() {
        assertThatThrownBy(() -> testData.from("users.json").forIndex(1).getJSONObject("invalid"))
                .isInstanceOf(TestDataException.class);
    }

    // ===========================================
    // getJSONArray Tests
    // ===========================================

    @Test
    public void testGetJSONArray_ValidJSON_ReturnsJSONArray() {
        var json = testData.from("users.json").forIndex(1).getJSONArray("tags");
        assertThat(json).isNotNull();
        assertThat(json.length()).isEqualTo(2);
        assertThat(json.getString(0)).isEqualTo("tag1");
    }

    @Test
    public void testGetJSONArray_NonExistingKey_ReturnsNull() {
        var json = testData.from("users.json").forIndex(1).getJSONArray("nonexistent");
        assertThat(json).isNull();
    }

    // ===========================================
    // getDescriptor Tests
    // ===========================================

    @Test
    public void testGetDescriptor_ExistingDescriptor_ReturnsValue() {
        String descriptor = testData.getDescriptor();
        assertThat(descriptor).isEqualTo("Test Description");
    }

    @Test
    public void testGetDescriptor_NullMap_ReturnsNull() {
        TestData emptyData = new TestData();
        String descriptor = emptyData.getDescriptor();
        assertThat(descriptor).isNull();
    }

    // ===========================================
    // getComment Tests
    // ===========================================

    @Test
    public void testGetComment_ExistingComment_ReturnsValue() {
        String comment = testData.getComment();
        assertThat(comment).isEqualTo("Test Comment");
    }

    @Test
    public void testGetComment_NullMap_ReturnsNull() {
        TestData emptyData = new TestData();
        String comment = emptyData.getComment();
        assertThat(comment).isNull();
    }

    // ===========================================
    // hasKey Tests
    // ===========================================

    @Test
    public void testHasKey_ExistingKey_ReturnsTrue() {
        boolean hasKey = testData.from("users.json").forIndex(1).hasKey("username");
        assertThat(hasKey).isTrue();
    }

    @Test
    public void testHasKey_NonExistingKey_ReturnsFalse() {
        boolean hasKey = testData.from("users.json").forIndex(1).hasKey("nonexistent");
        assertThat(hasKey).isFalse();
    }

    @Test
    public void testHasKey_NullMap_ReturnsFalse() {
        TestData emptyData = new TestData();
        boolean hasKey = emptyData.hasKey("anykey");
        assertThat(hasKey).isFalse();
    }

    // ===========================================
    // toString Tests
    // ===========================================

    @Test
    public void testToString_ContainsSourceFile() {
        testData.from("users.json").forIndex(1);
        String str = testData.toString();
        assertThat(str).contains("sourceFile='users.json'");
    }

    @Test
    public void testToString_ContainsIndex() {
        testData.from("users.json").forIndex(2);
        String str = testData.toString();
        assertThat(str).contains("index=2");
    }

    @Test
    public void testToString_ContainsDescriptor() {
        String str = testData.toString();
        assertThat(str).contains("descriptor='Test Description'");
    }
}
