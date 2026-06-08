package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.BaseComponent;
import ai.nextgpu.common.model.MemoryModule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BaseComponentDto extends BaseDto{
    private String manufacturer;

    private String model;

    private Integer yearReleased;

    private Boolean isDiscontinued;

    private Integer tdpWatts;

    private String productIdentifier;
}
