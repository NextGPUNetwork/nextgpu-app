package ai.nextgpu.common.dto;

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
public class CreateStorageDto {

    @NotBlank(message = "Serial number is required")
    private String serialNumber;
    
    @NotBlank(message = "Type is required")
    private String type;
    
    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;
    
    private String model;
    
    @NotNull(message = "Capacity is required")
    @Min(value = 0, message = "Capacity must be positive")
    private Long capacity;
    
    private String status;

    private Boolean isActive;

    private String location;

    private String notes;
}
