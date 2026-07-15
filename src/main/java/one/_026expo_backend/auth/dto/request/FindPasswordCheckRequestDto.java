package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "비밀번호 찾기(재설정) - 인증 번호 검증 및 유저 정보 조회 요청 DTO")
public class FindPasswordCheckRequestDto extends EmailCheckRequestDto {
    @NotBlank(message = "아이디 입력은 필수입니다.")
    @Schema(description = "비밀번호를 재설정하려는 아이디(본인의 아이디)", example = "your-id")
    private String loginId;
}
