package ai.nextgpu.common.dto;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class ConnectedToDto {
    private String connectedEntityId;
    private LocalDateTime connectionStart;
    private String role;
}
