package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.NetworkInterfaceType;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class CreateNetworkDeviceDto extends BaseComponentDto {

    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$",
            message = "Invalid MAC address format")
    private String macAddress;

    private NetworkInterfaceType interfaceType;

    private int speed;
}
