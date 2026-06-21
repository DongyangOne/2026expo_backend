package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "회원탈퇴 응답 DTO")
public class UserWithdrawResponseDto {

    @Schema(description = "회원탈퇴 완료 메시지", example = "탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.")
    private String message;

    public static UserWithdrawResponseDto of(String message) {
        return UserWithdrawResponseDto.builder()
                .message(message)
                .build();
    }
}
