package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;

@Getter
@NoArgsConstructor
@Schema(description = "KAKAO 로그인 요청 DTO")
public class KakaoLoginRequestDto {

    @NotBlank(message = "카카오 인가 코드는 필수입니다.")
    @Schema(description = "카카오 인가 코드", example = "7y4ZJpT1...")
    private String code;

    @NotBlank(message = "카카오 redirectUri는 필수입니다.")
    // redirectUri는 프론트 개발 진행사항에 따라 달라질 수 있으며, 앱 플랫폼 키 > 카카오 로그인 리다이렉트 URI 와 동일해야 함
    @Schema(description = "카카오 redirect URI", example = "http://localhost:3000/auth/kakao/callback")
    private String redirectUri;

    @NotNull(message = "자동 로그인 여부는 필수입니다.")
    @Schema(description = "자동 로그인 여부", example = "Y")
    private UseYnEnum rememberMe;
}