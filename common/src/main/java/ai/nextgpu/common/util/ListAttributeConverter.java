package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Converter
public class ListAttributeConverter implements AttributeConverter<List<String>, String> {

    private static final Logger log = LoggerFactory.getLogger(ListAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(List<String> list) {
        if (list == null) {
            return null;
        }
        try {
            return JsonUtil.OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException jpe) {
            log.warn("Cannot convert list into JSON. Error: {}", jpe.getMessage());
            return null;
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return JsonUtil.OBJECT_MAPPER.readValue(value, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Cannot convert JSON into list. Error: {}", e.getMessage());
            return null;
        }
    }
}
