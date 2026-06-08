package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.MemoryType;
import ai.nextgpu.common.model.StorageUnit;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryModuleDto {

    @NotBlank(message = "Type is required")
    private MemoryType type;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be greater than 0")
    private Integer capacity;

    @NotNull(message = "CapacityUnit is required")
    private StorageUnit capacityUnit;

    @Min(value = 1, message = "BusSpeed must be greater than 0")
    private Integer busSpeed;
}
