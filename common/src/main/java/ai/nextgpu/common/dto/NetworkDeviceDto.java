package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.NetworkDevice;
import ai.nextgpu.common.model.NetworkInterfaceType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkDeviceDto extends BaseComponentDto {

    private NetworkInterfaceType interfaceType;

    private String macAddress;

    private int speed;

    public static NetworkDevice toEntity (NetworkDeviceDto networkDeviceDto){
        if(networkDeviceDto == null) return null;

        NetworkDevice networkDevice = new NetworkDevice();
        BeanUtils.copyProperties(networkDeviceDto, networkDevice);

        return networkDevice;
    }

    public static NetworkDeviceDto toDto(NetworkDevice networkDevice) {
        return NetworkDeviceDto.builder()
                .uuid(networkDevice.getUuid())
                .model(networkDevice.getModel())
                .manufacturer(networkDevice.getManufacturer())
                .yearReleased(networkDevice.getYearReleased())
                .isDiscontinued(networkDevice.getIsDiscontinued())
                .tdpWatts(networkDevice.getTdpWatts())
                .productIdentifier(networkDevice.getProductIdentifier())
                .dateCreated(networkDevice.getDateCreated())
                .voided(networkDevice.getVoided())
                .dateVoided(networkDevice.getDateVoided())
                .dateUpdated(networkDevice.getDateUpdated())
                .voidReason(networkDevice.getVoidReason())
                .interfaceType(networkDevice.getInterfaceType())
                .macAddress(networkDevice.getMacAddress())
                .speed(networkDevice.getSpeed())
                .build();
    }
}
