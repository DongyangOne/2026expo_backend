package one._026expo_backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "마이페이지 사용자 인증 이메일 코드 검증 요청 DTO")
public class UserEmailVerificationConfirmRequestDto {

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(
            description = "인증 코드를 수신한 로그인 사용자 계정 이메일",
            example = "user@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "인증 코드를 입력해 주세요.")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다.")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 숫자 6자리입니다.")
    @Schema(
            description = "이메일로 발송된 6자리 인증 코드",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String verificationCode;
}
