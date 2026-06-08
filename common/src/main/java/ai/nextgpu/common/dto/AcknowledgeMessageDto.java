package ai.nextgpu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcknowledgeMessageDto {
    private String messageId;
    private String messageHash;
    private String acknowledgedBy;
}
