package ai.nextgpu.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseMetaDataDto {
    private String uuid;

    private String name;

    private String description;

    private String datatype;

    private Integer version;

    private LocalDateTime dateCreated;

    private LocalDateTime dateUpdated;

    private boolean retired;

    private LocalDateTime dateRetired;

    private String retireReason;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BaseMetaDataDto baseMetadataDto = (BaseMetaDataDto) o;
        return retired == baseMetadataDto.retired
                && Objects.equals(uuid, baseMetadataDto.uuid)
                && Objects.equals(dateCreated, baseMetadataDto.dateCreated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, dateCreated, retired);
    }
}
