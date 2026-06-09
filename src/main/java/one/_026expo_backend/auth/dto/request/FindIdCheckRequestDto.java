package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "아이디 찾기 - 인증 번호 검증 및 ID 조회 요청 DTO")
public class FindIdCheckRequestDto extends EmailCheckRequestDto {
    /**
     * EmailCheckRequestDto의 email, authCode 필드 및 @Size 등 검증 로직을 상속받음
     */
}