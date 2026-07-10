package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDto extends BaseDto {

    private String username;

    private String walletAddress;

    private String email;

    private String city;

    private String country;

    private Role role;

    private Map<String, String> attributes;

    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setUuid(user.getUuid());
        dto.setName(user.getName());
        dto.setDateCreated(user.getDateCreated());
        dto.setVoided(user.getVoided());
        dto.setDateVoided(user.getDateVoided());
        dto.setDateUpdated(user.getDateUpdated());
        dto.setVoidReason(user.getVoidReason());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());

        if (user instanceof Provider provider) {
            dto.setWalletAddress(provider.getWalletAddress());
            dto.setEmail(provider.getProviderEmail());
            dto.setCity(provider.getCity());
            dto.setCountry(provider.getCountry());
            dto.setAttributes(ProviderDto.toProviderAttributeDtosMap(provider.getProviderAttributes()));
        } else if (user instanceof Consumer consumer) {
            dto.setWalletAddress(consumer.getWalletAddress());
        }

        return dto;
    }
}