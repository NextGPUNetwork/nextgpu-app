package ai.nextgpu.common.dto;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseDto {

    private String uuid;

    private String name;

    private LocalDateTime dateCreated;

    private boolean voided;

    private LocalDateTime dateVoided;

    private LocalDateTime dateUpdated;

    private String voidReason;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BaseDto baseDto = (BaseDto) o;
        return voided == baseDto.voided && Objects.equals(uuid, baseDto.uuid) && Objects.equals(dateCreated, baseDto.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, dateCreated, voided);
    }
}
