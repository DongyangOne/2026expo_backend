package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "비밀번호 찾기(재설정) - 비밀번호 재설정 요청 DTO")
public class PasswordResetRequestDto {
    @Schema(description = "발급받은 비밀번호 재설정 임시 권한용 토큰 (UUID)",
            example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    @NotBlank(message = "비밀번호 재설정 토큰은 필수입니다.")
    private String passwordResetToken;

    @Schema(description = "재설정 하려는 비밀번호", example = "newPassword123*")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    private String newPassword;
}
