package ai.nextgpu.common.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class ListAttributeConverterTest {

    private final ListAttributeConverter converter = new ListAttributeConverter();

    @Test
    public void testConvertToDatabaseColumn() {
        List<String> list = Arrays.asList("task1", "task2", "task3");
        String json = converter.convertToDatabaseColumn(list);
        assertEquals("[\"task1\",\"task2\",\"task3\"]", json);
    }

    @Test
    public void testConvertToDatabaseColumn_Null() {
        assertNull(converter.convertToDatabaseColumn(null));
    }

    @Test
    public void testConvertToEntityAttribute() {
        String json = "[\"task1\",\"task2\"]";
        List<String> list = converter.convertToEntityAttribute(json);
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("task1", list.get(0));
        assertEquals("task2", list.get(1));
    }

    @Test
    public void testConvertToEntityAttribute_Null() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
    }

    @Test
    public void testConvertToEntityAttribute_InvalidJson() {
        assertNull(converter.convertToEntityAttribute("{invalid}"));
    }
}
