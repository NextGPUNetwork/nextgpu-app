package ai.nextgpu.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import ai.nextgpu.common.report.AnomalyDetail;

@Converter
public class AnomalyDetailListAttributeConverter implements AttributeConverter<List<AnomalyDetail>, String> {

    private static final Logger log = LoggerFactory.getLogger(AnomalyDetailListAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(List<AnomalyDetail> list) {
        if (list == null) return null;
        try {
            return JsonUtil.OBJECT_MAPPER.writeValueAsString(list);
        } catch (JsonProcessingException jpe) {
            log.warn("Cannot convert anomaly detail list into JSON. Error: {}", jpe.getMessage());
            return null;
        }
    }

    @Override
    public List<AnomalyDetail> convertToEntityAttribute(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return JsonUtil.OBJECT_MAPPER.readValue(value, new TypeReference<List<AnomalyDetail>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Cannot convert JSON into anomaly detail list. Error: {}", e.getMessage());
            return null;
        }
    }
}

