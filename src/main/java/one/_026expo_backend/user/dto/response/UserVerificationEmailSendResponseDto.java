package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 마이페이지 사용자 인증 이메일 전송 응답 DTO입니다.
 *
 * 실제 이메일 전체를 그대로 노출하지 않고, 화면에 필요한 최소 정보만 내려줍니다.
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "마이페이지 사용자 인증 이메일 전송 응답 DTO")
public class UserVerificationEmailSendResponseDto {

    @Schema(description = "마스킹된 이메일 주소", example = "c****6@naver.com")
    private String maskedEmail;

    @Schema(description = "인증 코드 유효 시간(초)", example = "300")
    private Integer expiresInSeconds;

    public static UserVerificationEmailSendResponseDto of(String maskedEmail, Integer expiresInSeconds) {
        return UserVerificationEmailSendResponseDto.builder()
                .maskedEmail(maskedEmail)
                .expiresInSeconds(expiresInSeconds)
                .build();
    }
}
