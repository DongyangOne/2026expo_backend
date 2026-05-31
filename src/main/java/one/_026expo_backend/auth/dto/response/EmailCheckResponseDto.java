package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "이메일 인증 번호 검증 응답 DTO")
public class EmailCheckResponseDto {

    @Schema(description = "인증 성공 여부", example = "true")
    private boolean isVerified;

    @Schema(description = "응답 메시지", example = "이메일 인증이 완료되었습니다.")
    private String message;

    public static EmailCheckResponseDto of(boolean isVerified, String message) {
        return EmailCheckResponseDto.builder()
                .isVerified(isVerified)
                .message(message)
                .build();
    }
}