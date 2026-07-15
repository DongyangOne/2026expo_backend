package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "이메일 인증 번호 검증 요청 DTO")
public class EmailCheckRequestDto {
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(
            description = "인증 번호를 수신한 메일 주소(본인의 메일 주소)",
            example = "your-email@naver.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String email;

    @NotBlank(message = "인증 번호는 필수 입력 항목입니다.")
    @Size(min = 6, max = 6, message = "인증 번호는 6자리여야 합니다.")
    @Schema(
            description = "이메일로 발송된 6자리 인증 번호",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String authCode;
}