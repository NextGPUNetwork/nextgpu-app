package ai.nextgpu.common.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseMetadataTest {

    private DummyClass testMetadata;

    @BeforeEach
    void setUp() {
        testMetadata = new DummyClass();
        testMetadata.setName("LAST_CPU_BENCHMARK_SCORE");
        testMetadata.setDatatype("java.lang.Integer");
        testMetadata.setVersion(1);
    }

    @Test
    void shouldGetCleanValuePrimitiveTypes() {
        testMetadata.setDatatype("java.lang.Integer");
        Object intObject = testMetadata.getCleanValue("42");
        assertEquals(42, intObject);

        testMetadata.setDatatype("java.lang.Boolean");
        assertEquals(true, testMetadata.getCleanValue("true"));

        testMetadata.setDatatype("java.lang.Double");
        assertEquals(3.14, testMetadata.getCleanValue("3.14"), 0.0001);

        testMetadata.setDatatype("java.lang.Float");
        assertEquals(5.5f, testMetadata.getCleanValue("5.5"), 0.0001f);

        testMetadata.setDatatype("java.lang.Long");
        Long longObject = testMetadata.getCleanValue("100000000");
        assertEquals(100000000L, longObject);

        testMetadata.setDatatype("java.lang.Short");
        short shortObject = testMetadata.getCleanValue("7");
        assertEquals(7, shortObject);

        testMetadata.setDatatype("java.lang.Byte");
        Byte byteObject = testMetadata.getCleanValue("7");
        assertEquals((byte) 7, byteObject);

        testMetadata.setDatatype("java.lang.Character");
        assertEquals('@', (char) testMetadata.getCleanValue("@"));

        testMetadata.setDatatype("java.lang.String");
        assertEquals("Hello", testMetadata.getCleanValue("Hello"));
    }

    @Test
    void shouldNotGetCleanValue() {
        Exception exception = assertThrows(RuntimeException.class, () ->
                testMetadata.getCleanValue("NaN"));
        assertTrue(exception.getMessage().contains("Failed to convert value"));
    }

    @Test
    void testGetCleanValue_JsonConversion() {
        String json = "{\"id\": 1,\n" +
                "  \"name\": \"Temperature Sensor\",\n" +
                "  \"manufacturer\": \"Samsung\",\n" +
                "  \"model\": \"970 EVO Plus\",\n" +
                "  \"yearReleased\": 2019,\n" +
                "  \"isDiscontinued\": false,\n" +
                "  \"tdpWatts\": 6,\n" +
                "  \"productIdentifier\": \"MZ-V7S1T0B\",\n" +
                "  \"type\": \"SENSOR\",\n" +
                "  \"specificationKey\": \"Capacity\",\n" +
                "  \"specificationValue\": \"1TB\"\n" +
                "}\n";
        ComputerAttributeType avgTemp = ComputerAttributeType.builder()
                .isMandatory(true)
                .isSearchable(true)
                .isUnique(false)
                .build();
        avgTemp.setName("Average Temperature");
        avgTemp.setVersion(1);
        avgTemp.setDatatype(GenericComponent.class.getCanonicalName());
        Object genericComponent = avgTemp.getCleanValue(json);
        assertNotNull(genericComponent);
        assertInstanceOf(GenericComponent.class, genericComponent);
    }
}

// Helper class for testing
class DummyClass extends BaseMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
}
