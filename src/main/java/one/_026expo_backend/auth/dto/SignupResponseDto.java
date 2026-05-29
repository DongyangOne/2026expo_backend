package one._026expo_backend.auth.dto;

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
@Schema(description = "회원가입 응답 DTO")
public class SignupResponseDto {
	@Schema(description = "이름", example = "홍길동")
	private String username;

    @Schema(description = "로그인 아이디", example = "user123")
    private String loginId;

	@Schema(description = "이메일", example = "user@example.com")
	private String email;

	@Schema(description = "생성일시", example = "2026-05-28T08:12:00")
	private LocalDateTime createdDate;

}
