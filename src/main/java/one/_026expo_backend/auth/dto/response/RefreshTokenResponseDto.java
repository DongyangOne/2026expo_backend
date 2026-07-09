package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "리프레시 토큰 재발급 응답 DTO")
public class RefreshTokenResponseDto {

    @Schema(description = "AccessToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @Schema(description = "RefreshToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;

    public static RefreshTokenResponseDto of(String accessToken, String refreshToken) {
        return RefreshTokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}