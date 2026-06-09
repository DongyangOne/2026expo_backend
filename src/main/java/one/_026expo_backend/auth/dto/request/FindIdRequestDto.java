package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "아이디 찾기 - 인증 번호 검증 요청 DTO")
public class FindIdRequestDto  extends EmailSendRequestDto{
    /**
     * EmailSendRequestDto와 중복 코드이므로
     * 여기에선 상속만 받음
     */
}
