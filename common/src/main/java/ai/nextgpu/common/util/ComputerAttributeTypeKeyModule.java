package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ai.nextgpu.common.model.ComputerAttributeType;

import java.io.IOException;

public class ComputerAttributeTypeKeyModule extends SimpleModule {
    public ComputerAttributeTypeKeyModule() {
        addKeySerializer(ComputerAttributeType.class, new ComputerAttributeTypeKeySerializer());
        addKeyDeserializer(ComputerAttributeType.class, new ComputerAttributeTypeKeyDeserializer());
    }

    private static class ComputerAttributeTypeKeySerializer extends StdSerializer<ComputerAttributeType> {
        protected ComputerAttributeTypeKeySerializer() {
            super(ComputerAttributeType.class);
        }

        @Override
        public void serialize(ComputerAttributeType value, JsonGenerator gen, SerializerProvider provider) 
            throws IOException {
            gen.writeFieldName(value.getName());
        }
    }

    private static class ComputerAttributeTypeKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) {
            ComputerAttributeType type = new ComputerAttributeType();
            type.setName(key);
            return type;
        }
    }
}
