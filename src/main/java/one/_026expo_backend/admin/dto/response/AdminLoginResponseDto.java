package one._026expo_backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "관리자 로그인 응답 DTO")
public class AdminLoginResponseDto {
    @Schema(description = "관리자 로그인 아이디", example = "admin123")
    private String adminLoginId;

    @Schema(description = "관리자 소속", example = "운영지원팀")
    private String team;

    @Schema(description = "AdminAccessToken", example = "eyJhbGciOiJIUz...")
    private String adminAccessToken;

    @Schema(description = "AdminRefreshToken", example = "eyJhbGciOiJIUz...")
    private String adminRefreshToken;
}
