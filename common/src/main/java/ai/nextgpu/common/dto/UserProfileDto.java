package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserProfileDto extends BaseDto {

    private String username;

    private String walletAddress;

    private String email;

    private Role role;
}
