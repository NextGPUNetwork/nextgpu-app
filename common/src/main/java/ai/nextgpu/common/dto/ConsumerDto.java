package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Consumer;
import ai.nextgpu.common.model.Gpu;
import ai.nextgpu.common.model.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ConsumerDto extends BaseDto {

    private String username;

    private String walletAddress;

    private Role role;

    public static ConsumerDto toDto(Consumer consumer){
        return ConsumerDto.builder()
                .username(consumer.getUsername())
                .walletAddress(consumer.getWalletAddress())
                .role(Role.CONSUMER)
                .uuid(consumer.getUuid())
                .name(consumer.getName())
                .dateCreated(consumer.getDateCreated())
                .voided(consumer.getVoided())
                .dateVoided(consumer.getDateVoided())
                .dateUpdated(consumer.getDateUpdated())
                .voidReason(consumer.getVoidReason())
                .build();
    }

    public static Consumer toEntity(ConsumerDto dto){
        Consumer consumer = new Consumer();
        if (dto == null) return null;

        BeanUtils.copyProperties(dto, consumer);
        return consumer;
    }
}
