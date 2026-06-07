package one._026expo_backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "관리자 토큰 재발급 응답 DTO")
public class AdminRefreshTokenResponseDto {
    @Schema(description = "새로운 액세스 토큰")
    private String accessToken;

    @Schema(description = "새로운 리프레시 토큰")
    private String refreshToken;
}
