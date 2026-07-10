package ai.nextgpu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StructuredAiRequestDto {

    private String context;

    private String prompt;

    private Map<String, String> schema;
}