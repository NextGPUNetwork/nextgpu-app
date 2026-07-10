package ai.nextgpu.common.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SttDto {
    private String text;
    private String language;
    @JsonSetter("language_probability")
    private String languageProbability;
}
