package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Converter
public class MapAttributeConverter implements AttributeConverter<Map<String, Object>, String> {

    private static final Logger log = LoggerFactory.getLogger(MapAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(Map map) {
        try {
            return JsonUtil.OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException jpe) {
            log.warn("Cannot convert map into JSON. Error: {}", jpe.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> convertToEntityAttribute(String value) {
        try {
            return JsonUtil.OBJECT_MAPPER.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Cannot convert map into JSON. Error: {}", e.getMessage());
            return null;
        }
    }
}
