package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.model.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ProviderDto extends BaseDto {

    private String username;

    private String walletAddress;

    private String providerEmail;

    private String city;

    private String country;

    private Role role;

    public static ProviderDto toDto(Provider provider){
        return ProviderDto.builder()
                .username(provider.getUsername())
                .walletAddress(provider.getWalletAddress())
                .providerEmail(provider.getProviderEmail())
                .city(provider.getCity())
                .country(provider.getCountry())
                .role(Role.PROVIDER)
                .uuid(provider.getUuid())
                .name(provider.getName())
                .dateCreated(provider.getDateCreated())
                .voided(provider.getVoided())
                .dateVoided(provider.getDateVoided())
                .dateUpdated(provider.getDateUpdated())
                .voidReason(provider.getVoidReason())
                .build();
    }

    public static Provider toEntity(ProviderDto dto){
        Provider provider = new Provider();
        if (dto == null) return null;
        BeanUtils.copyProperties(dto, provider);

        return provider;
    }
}
