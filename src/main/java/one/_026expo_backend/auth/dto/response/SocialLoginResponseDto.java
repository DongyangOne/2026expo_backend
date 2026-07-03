package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.user.enums.SocialType;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "소셜 로그인 응답 DTO")
public class SocialLoginResponseDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "소셜 제공 식별 아이디", example = "1234567890")
    private String socialProviderId;

    @Schema(description = "소셜 로그인 타입", example = "KAKAO")
    private SocialType socialType;

    @Schema(description = "이름", example = "홍길동")
    private String username;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "회원가입 필요 여부", example = "false")
    private UseYnEnum needsSignup;

    @Schema(description = "자동 로그인 여부", example = "Y")
    private UseYnEnum rememberMe;

    @Schema(description = "AccessToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String accessToken;

    @Schema(description = "RefreshToken", example = "eyJhbGciOiJIUzUxMiJ9...")
    private String refreshToken;

    // 로그인 시 사용
    public static SocialLoginResponseDto of(
        Long userId,
        String socialProviderId,
        SocialType socialType,
        String username,
        String email,
        UseYnEnum rememberMe,
        String accessToken,
        String refreshToken
    ) {
        return SocialLoginResponseDto.builder()
            .userId(userId)
            .socialProviderId(socialProviderId)
            .socialType(socialType)
            .username(username)
            .email(email)
            .needsSignup(UseYnEnum.N)
            .rememberMe(rememberMe)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }

    // 회원가입 필요 시 사용
    public static SocialLoginResponseDto signupRequired(
            String socialProviderId,
            SocialType socialType,
            String username,
            String email
    ) {
         return SocialLoginResponseDto.builder()
            .socialProviderId(socialProviderId)
            .socialType(socialType)
            .username(username)
            .email(email)
            .needsSignup(UseYnEnum.Y)
            .build();
    }
}