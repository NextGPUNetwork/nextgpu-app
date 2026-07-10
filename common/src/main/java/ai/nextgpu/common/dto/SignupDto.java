package ai.nextgpu.common.dto;

import ai.nextgpu.common.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SignupDto {

    @NotBlank
    private String walletAddress;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String city;

    @NotBlank
    private String country;

    @NotNull
    private Role role;

    @NotBlank
    String message;

    @NotBlank
    String nonce;

    @NotBlank
    String signature;
}
