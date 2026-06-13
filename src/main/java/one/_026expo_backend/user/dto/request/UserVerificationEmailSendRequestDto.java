package one._026expo_backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마이페이지 사용자 인증 이메일 전송 요청 DTO입니다.
 *
 * 현재는 로그인한 사용자의 계정 이메일을 서버가 직접 조회하므로 요청 본문 필드를 두지 않습니다.
 * 빈 DTO를 유지해 추후 확장 시 API 시그니처를 바꾸지 않도록 합니다.
 */
@Getter
@NoArgsConstructor
@Schema(description = "마이페이지 사용자 인증 이메일 전송 요청 DTO")
public class UserVerificationEmailSendRequestDto {
}
