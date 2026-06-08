package ai.nextgpu.common.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import ai.nextgpu.common.util.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BaseJsonTest {

    /**
     * Test description:
     * Ensures `setData` behaves correctly when a null Provider is passed.
     */
    @Test
    void testPutWithNullObject() {
        BaseJson baseJson = new BaseJson(JsonNodeFactory.instance);
        Provider provider = null;

        Exception exception = assertThrows(IllegalArgumentException.class, () -> baseJson.put(provider));
        assertEquals("Data object must be serializable", exception.getMessage());
    }

    /**
     * Test description:
     * Verifies that the `put` method correctly handles a Provider entity
     * and ensures the data can be retrieved.
     */
    @Test
    void testPutAndFetchProvider() {
        BaseJson baseJson = new BaseJson(JsonNodeFactory.instance);
        Provider provider = new Provider();
        provider.setName("Test Provider");
        baseJson.put(provider);
        JsonNode dataNode = baseJson.get("data");
        assertNotNull(dataNode);
        Provider retrieved = JsonUtil.OBJECT_MAPPER.convertValue(dataNode, Provider.class);
        assertEquals(provider.getUuid(), retrieved.getUuid());
    }

    /**
     * Test description:
     * Verifies that the `put` method correctly handles a List of strings
     * and ensures the data can be retrieved.
     */
    @Test
    void testPutAndFetchStringList() {
        BaseJson baseJson = new BaseJson(JsonNodeFactory.instance);
        List<String> stringList = Arrays.asList("test1", "test2", "test3");

        baseJson.put(stringList);
        JsonNode dataNode = baseJson.get("data");

        assertNotNull(dataNode);
        List<String> deserializedList = JsonUtil.OBJECT_MAPPER.convertValue(dataNode, List.class);
        assertTrue(deserializedList.containsAll(Arrays.asList("test1", "test2", "test3")));
    }
}
