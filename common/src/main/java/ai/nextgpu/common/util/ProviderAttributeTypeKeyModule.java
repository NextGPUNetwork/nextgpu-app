package ai.nextgpu.common.util;

import ai.nextgpu.common.model.ProviderAttributeType;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

public class ProviderAttributeTypeKeyModule extends SimpleModule {

    public ProviderAttributeTypeKeyModule() {
        addKeySerializer(ProviderAttributeType.class, new ProviderAttributeTypeKeySerializer());
        addKeyDeserializer(ProviderAttributeType.class, new ProviderAttributeTypeKeyDeserializer());
    }

    private static class ProviderAttributeTypeKeySerializer extends StdSerializer<ProviderAttributeType> {
        protected ProviderAttributeTypeKeySerializer() {
            super(ProviderAttributeType.class);
        }

        @Override
        public void serialize(ProviderAttributeType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeFieldName(value.getName());
        }
    }

    private static class ProviderAttributeTypeKeyDeserializer extends KeyDeserializer {
        @Override
        public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
            ProviderAttributeType type = new ProviderAttributeType();
            type.setName(key);
            return type;
        }
    }
}
