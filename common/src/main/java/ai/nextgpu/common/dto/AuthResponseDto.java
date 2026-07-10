package ai.nextgpu.common.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AuthResponseDto {
    private String accessToken;
    private Date accessTokenExpiry;
    private UserDto userDto;
}
