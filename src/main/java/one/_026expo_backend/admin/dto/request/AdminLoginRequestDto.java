package one._026expo_backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 로그인 요청 DTO")
public class AdminLoginRequestDto {
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 12, message = "아이디는 4자 이상 12자 이하여야 합니다.")
    @Schema(description = "관리자 로그인 아이디", example = "admin123")
    private String adminLoginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    @Schema(description = "관리자 비밀번호", example = "password123*")
    private String adminPassword;
}
