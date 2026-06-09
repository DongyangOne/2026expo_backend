package one._026expo_backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "관리자 회원가입 응답 DTO")
public class AdminSignupResponseDto {

    @Schema(description = "관리자 로그인 아이디", example = "admin123")
    private String adminId;

    @Schema(description = "소속 팀명", example = "운영기획팀")
    private String team;

    @Schema(description = "생성일시", example = "2026-06-01T00:00:00")
    private LocalDateTime createdDate;
}