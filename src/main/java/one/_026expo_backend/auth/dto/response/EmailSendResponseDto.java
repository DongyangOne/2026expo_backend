package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "이메일 인증번호 발송 응답 정보")
public class EmailSendResponseDto {
    @Schema(description = "인증번호가 발송된 이메일 주소", example = "user@example.com")
    private String email;
    @Schema(description = "인증번호 만료 시간(발송 후 5분)", example = "2026-05-31T00:12:00")
    private LocalDateTime expiredAt;

    public static EmailSendResponseDto of(String email, LocalDateTime expiredAt) {
        return EmailSendResponseDto.builder()
                .email(email)
                .expiredAt(expiredAt)
                .build();
    }
}