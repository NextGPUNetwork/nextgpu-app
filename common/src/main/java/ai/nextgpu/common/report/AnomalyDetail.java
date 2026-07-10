package ai.nextgpu.common.report;

import ai.nextgpu.common.model.BaseComponent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyDetail {
    private String errorCode;          // e.g., "MAX_TOLERANCE_VIOLATION"
    private String message;            // Human-readable description
    private String componentType;      // e.g., "Cpu", "Gpu", "Storage"
}
