package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Provider;
import ai.nextgpu.common.model.ProviderAttributeType;
import ai.nextgpu.common.model.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    private Map<String, String> providerAttributes;

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
                .providerAttributes(toProviderAttributeDtosMap(provider.getProviderAttributes()))
                .build();
    }

    public static Provider toEntity(ProviderDto dto){
        Provider provider = new Provider();
        if (dto == null) return null;
        BeanUtils.copyProperties(dto, provider);

        return provider;
    }

    public static Map<String, String> toProviderAttributeDtosMap(
            Map<ProviderAttributeType, String> attributeMap) {

        if (attributeMap == null || attributeMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // Create new map
        Map<String , String> result = new HashMap<>();

        // Iterate and convert
        for (Map.Entry<ProviderAttributeType, String> entry : attributeMap.entrySet()) {
            result.put(entry.getKey().getName(), entry.getValue());
        }

        return result;
    }

}
