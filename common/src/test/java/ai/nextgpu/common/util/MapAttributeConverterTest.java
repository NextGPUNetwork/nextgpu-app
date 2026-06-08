package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class MapAttributeConverterTest {

    /**
     * Tests the convertToDatabaseColumn method of MapAttributeConverter.
     * This method converts a Map<String, Object> to a JSON string
     * using the JsonUtil.OBJECT_MAPPER.
     */
    @Test
    public void testConvertToDatabaseColumn_ValidMap() throws JsonProcessingException {
        // Arrange
        MapAttributeConverter converter = new MapAttributeConverter();
        Map<String, Object> testMap = Map.of("key1", "value1", "key2", 123);
        String expectedJson = "{\"key1\":\"value1\",\"key2\":123}";

        // Act
        String result = converter.convertToDatabaseColumn(testMap);

        // Assert
        assertEquals(expectedJson, result);
    }

    @Test
    public void testConvertToDatabaseColumn_Null() {
        // Arrange
        MapAttributeConverter converter = new MapAttributeConverter();

        // Act
        String result = converter.convertToDatabaseColumn(null);

        // Assert
        assertNull(result);
    }

    @Test
    public void testConvertToDatabaseColumn_InvalidMap() throws JsonProcessingException {
        // Arrange
        MapAttributeConverter converter = spy(new MapAttributeConverter());
        JsonProcessingException exception = new JsonProcessingException("Test error") {
        };
        doThrow(exception).when(JsonUtil.OBJECT_MAPPER).writeValueAsString(any());
        Map<String, Object> testMap = Map.of("key", "value");

        // Act
        String result = converter.convertToDatabaseColumn(testMap);

        // Assert
        assertNull(result);
    }
}
