package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.ComputerAttributeType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ComputerAttributeTypeDto extends BaseMetaDataDto {

    private Boolean isSearchable;

    private Boolean isMandatory;

    private Boolean isUnique;

    private String validationRegex;

    private Integer displayOrder;

    private String category;

    public static ComputerAttributeType toEntity (ComputerAttributeTypeDto attributeTypeDto){
        if(attributeTypeDto == null) return null;

        ComputerAttributeType attributeType = new ComputerAttributeType();
        BeanUtils.copyProperties(attributeTypeDto, attributeType);

        return attributeType;
    }

    public static ComputerAttributeTypeDto toDto(ComputerAttributeType attributeType) {
        return ComputerAttributeTypeDto.builder()
                .uuid(attributeType.getUuid())
                .name(attributeType.getName())
                .description(attributeType.getDescription())
                .datatype(attributeType.getDatatype())
                .version(attributeType.getVersion())
                .dateCreated(attributeType.getDateCreated())
                .dateUpdated(attributeType.getDateUpdated())
                .retired(attributeType.getRetired())
                .dateRetired(attributeType.getDateRetired())
                .retireReason(attributeType.getRetireReason())
                .isSearchable(attributeType.getIsSearchable())
                .isMandatory(attributeType.getIsMandatory())
                .isUnique(attributeType.getIsUnique())
                .validationRegex(attributeType.getValidationRegex())
                .displayOrder(attributeType.getDisplayOrder())
                .category(attributeType.getCategory())
                .build();
    }
}
