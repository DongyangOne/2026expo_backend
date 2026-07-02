package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.global.enums.UseYnEnum;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "로그인 응답 DTO")
public class LoginResponseDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "로그인 아이디", example = "user123")
    private String loginId;

    @Schema(description = "이름", example = "홍길동")
    private String username;

    @Schema(description = "자동 로그인 여부", example = "Y")
    private UseYnEnum rememberMe;

    @Schema(description = "AccessToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @Schema(description = "RefreshToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;

    public static LoginResponseDto of(Long userId, String loginId, String username, UseYnEnum rememberMe, String accessToken, String refreshToken) {
        return LoginResponseDto.builder()
                .userId(userId)
                .loginId(loginId)
                .username(username)
                .rememberMe(rememberMe)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
