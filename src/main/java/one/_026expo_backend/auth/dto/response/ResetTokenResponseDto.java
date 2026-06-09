package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "비밀번호 찾기(재설정) - 인증에 성공하여 임시 토큰 발급 응답 DTO")
public class ResetTokenResponseDto {
    @Schema(description = "비밀번호를 재설정하려는 유저의 고유 식별 아이디", example = "1")
    private Long id;

    @Schema(description = "비밀번호 재설정 임시 권한용 토큰 (UUID)",
            example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private String passwordResetToken;
}
