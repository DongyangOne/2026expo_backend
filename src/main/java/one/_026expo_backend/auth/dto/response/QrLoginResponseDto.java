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
    @Schema(description = "태블릿 AccessToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @Schema(description = "태블릿 RefreshToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;
}