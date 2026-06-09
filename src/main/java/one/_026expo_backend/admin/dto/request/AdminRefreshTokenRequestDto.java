package one._026expo_backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관리자 토큰 재발급 요청 DTO")
public class AdminRefreshTokenRequestDto {
    @Schema(description = "리프레시 토큰", example = "eyJhbGciOiJIUz...")
    @NotBlank(message = "리프레시 토큰은 필수 입력입니다.")
    private String refreshToken;
}
