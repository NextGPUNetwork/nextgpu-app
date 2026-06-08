package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.ComputerType;
import lombok.Data;
import java.util.Collection;
import java.util.Map;

@Data
public class ComputerSearchCriteria {
    private ComputerType type;
    private Collection<Long> cpuIds;
    private Collection<Long> gpuIds;
    private Collection<Long> memoryIds;
    private Collection<Long> storageIds;
    private String operatingSystem;
    private Collection<Long> networkDeviceIds;
    private Collection<Long> otherComponentIds;
}
