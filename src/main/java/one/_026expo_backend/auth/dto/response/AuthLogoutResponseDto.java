package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "로그아웃 응답 DTO")
public class AuthLogoutResponseDto {

    @Schema(description = "로그아웃 처리 결과 메시지", example = "로그아웃이 완료되었습니다.")
    private String message;

    public static AuthLogoutResponseDto of(String message) {
        return AuthLogoutResponseDto.builder()
                .message(message)
                .build();
    }
}
