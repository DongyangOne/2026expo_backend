package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;

@Getter
@NoArgsConstructor
@Schema(description = "소셜 로그인 요청 DTO")
public class SocialLoginRequestDto {

    @NotBlank(message = "인가 코드는 필수입니다.")
    @Schema(description = "인가 코드", example = "7y4ZJpT1...")
    private String code;

    @NotBlank(message = "redirectUri는 필수입니다.")
    @Schema(description = "redirect URI", example = "http://localhost:3000/auth/kakao/callback")
    private String redirectUri;

    @NotNull(message = "자동 로그인 여부는 필수입니다.")
    @Schema(description = "자동 로그인 여부", example = "Y")
    private UseYnEnum rememberMe;
}