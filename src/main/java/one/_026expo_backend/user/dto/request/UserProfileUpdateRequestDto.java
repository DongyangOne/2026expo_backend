package one._026expo_backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 프로필 수정 요청 DTO
 *
 * 이메일과 비밀번호 중 필요한 값만 보내 부분 수정할 수 있도록 optional 필드로 구성한다.
 */
@Getter
@NoArgsConstructor
@Schema(description = "마이페이지 프로필 수정 요청 DTO")
public class UserProfileUpdateRequestDto {

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Schema(description = "변경할 이메일", example = "newemail@example.com", nullable = true)
    private String email;

    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    @Schema(description = "변경할 비밀번호", example = "NewPassword123!", nullable = true)
    private String password;

    @Schema(description = "비밀번호 확인 값", example = "NewPassword123!", nullable = true)
    private String passwordConfirm;
}
