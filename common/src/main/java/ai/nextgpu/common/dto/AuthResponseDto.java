package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.User;
import lombok.Data;

import java.util.Date;

@Data
public class AuthResponseDto {
    private String accessToken;
    private Date accessTokenExpiry;
    private User user;
}
