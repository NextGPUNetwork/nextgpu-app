package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializers;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule())
            .registerModule(new ComputerAttributeTypeKeyModule());
}
