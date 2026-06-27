package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "마이페이지 사용자 인증 이메일 코드 검증 응답 DTO")
public class UserEmailVerificationConfirmResponseDto {

    @Schema(description = "인증 완료 여부", example = "true")
    private Boolean verified;

    @Schema(description = "응답 메시지", example = "이메일 인증이 완료되었습니다.")
    private String message;

    public static UserEmailVerificationConfirmResponseDto of(Boolean verified, String message) {
        return UserEmailVerificationConfirmResponseDto.builder()
                .verified(verified)
                .message(message)
                .build();
    }
}
