package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.model.ProviderAttributeType;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ProviderAttributeTypeDto extends BaseMetaDataDto {

    private Boolean isMandatory;

    private Boolean isUnique;

    private String validationRegex;

    public static ProviderAttributeTypeDto toDto(ProviderAttributeType attributeType) {
        if (attributeType == null) {
            return null;
        }

        return ProviderAttributeTypeDto.builder()
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
                .isMandatory(attributeType.getIsMandatory())
                .isUnique(attributeType.getIsUnique())
                .validationRegex(attributeType.getValidationRegex())
                .build();
    }

    public static ProviderAttributeType toEntity(ProviderAttributeTypeDto dto){
        ProviderAttributeType providerAttributeType = new ProviderAttributeType();
        if (dto == null) return null;
        BeanUtils.copyProperties(dto, providerAttributeType);

        return providerAttributeType;
    }
}