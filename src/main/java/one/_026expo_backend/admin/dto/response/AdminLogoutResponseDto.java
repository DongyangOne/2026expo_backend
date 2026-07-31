package one._026expo_backend.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "관리자 로그아웃 응답 DTO")
public class AdminLogoutResponseDto {

    @Schema(description = "로그아웃 처리 결과 메시지", example = "관리자 로그아웃이 완료되었습니다.")
    private String message;

    public static AdminLogoutResponseDto of(String message) {
        return AdminLogoutResponseDto.builder()
                .message(message)
                .build();
    }
}
