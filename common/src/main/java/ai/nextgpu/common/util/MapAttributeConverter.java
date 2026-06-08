package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Converter
@Slf4j
public class MapAttributeConverter implements AttributeConverter<Map<String, Object>, String> {

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
