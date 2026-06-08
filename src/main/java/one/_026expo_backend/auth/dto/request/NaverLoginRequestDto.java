package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;

@Getter
@NoArgsConstructor
@Schema(description = "NAVER 로그인 요청 DTO")
public class NaverLoginRequestDto {

    @NotBlank(message = "네이버 인가 코드는 필수입니다.")
    @Schema(description = "네이버 인가 코드", example = "abcd1234...")
    private String code;

    @NotBlank(message = "네이버 redirectUri는 필수입니다.")
    @Schema(description = "네이버 redirect URI", example = "http://localhost:3000/auth/naver/callback")
    private String redirectUri;

    @NotNull(message = "자동 로그인 여부는 필수입니다.")
    @Schema(description = "자동 로그인 여부", example = "Y")
    private UseYnEnum rememberMe;
}
