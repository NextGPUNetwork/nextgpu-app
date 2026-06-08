package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Computer;
import ai.nextgpu.common.model.ComputerType;
import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.model.GenericComponent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

import java.util.Collection;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComputerDto {
    @NotNull(message = "Serial number is required")
    private String walletAddress;

    private String uuid;

    @NotNull(message = "Computer type is required")
    private ComputerType type;
    
    @NotEmpty(message = "At least one GPU is required")
    private Collection<GpuDto> gpus;

    @NotEmpty(message = "At least one CPU is required")
    private Collection<CpuDto> cpus;

    @NotEmpty(message = "At least one memory is required")
    private Collection<MemoryModuleDto> memories;

    @NotEmpty(message = "At least one storage is required")
    private Collection<StorageDto> storages;

    @NotEmpty(message = "At least one operating system is required")
    private String operatingSystem;

    @NotEmpty(message = "At least one network device is required")
    private Collection<NetworkDeviceDto> networkDevices;

    private Collection<GenericComponentDto> otherComponents;

    private Map<String, String> computerAttributes;

    public CreateComputerDto fromComputer(Computer computer) {
        BeanUtils.copyProperties(computer, this);
        setWalletAddress(computer.getProvider().getWalletAddress());
        return this;
    }
}
