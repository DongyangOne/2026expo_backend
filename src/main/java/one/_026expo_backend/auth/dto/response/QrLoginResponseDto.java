package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "QR 로그인 승인 응답 DTO")
public class QrLoginResponseDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "로그인 아이디", example = "user123")
    private String loginId;

    @Schema(description = "이름", example = "홍길동")
    private String username;

    @Schema(description = "태블릿 AccessToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @Schema(description = "태블릿 RefreshToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
}