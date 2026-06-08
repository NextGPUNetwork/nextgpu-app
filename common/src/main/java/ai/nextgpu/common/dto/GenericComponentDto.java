// GenericComponentDto.java
package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.DeviceType;
import ai.nextgpu.common.model.GenericComponent;
import jakarta.persistence.Column;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Comment;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class GenericComponentDto extends BaseComponentDto {

    private DeviceType type;

    private String specificationKey;

    private String specificationValue;

    public static GenericComponent toEntity (GenericComponentDto genericComponentDto){
        if(genericComponentDto == null) return null;

        GenericComponent genericComponent = new GenericComponent();
        BeanUtils.copyProperties(genericComponentDto, genericComponent);

        return genericComponent;
    }

    public static GenericComponentDto toDto(GenericComponent otherComponent) {
        return GenericComponentDto.builder()
                .uuid(otherComponent.getUuid())
                .model(otherComponent.getModel())
                .manufacturer(otherComponent.getManufacturer())
                .yearReleased(otherComponent.getYearReleased())
                .isDiscontinued(otherComponent.getIsDiscontinued())
                .tdpWatts(otherComponent.getTdpWatts())
                .productIdentifier(otherComponent.getProductIdentifier())
                .dateCreated(otherComponent.getDateCreated())
                .voided(otherComponent.getVoided())
                .dateVoided(otherComponent.getDateVoided())
                .dateUpdated(otherComponent.getDateUpdated())
                .voidReason(otherComponent.getVoidReason())
                .type(otherComponent.getType())
                .specificationKey(otherComponent.getSpecificationKey())
                .build();
    }
}
