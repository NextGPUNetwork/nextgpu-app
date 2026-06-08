// CreateGenericComponentDto.java
package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGenericComponentDto {

    @NotNull(message = "Type is required")
    private DeviceType type;

    @NotBlank(message = "Specification Key is required")
    private String specificationKey;

    @NotBlank(message = "Specification Value is required")
    private String specificationValue;
}
