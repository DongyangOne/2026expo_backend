package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "아이디 찾기 결과 응답 DTO")
public class FindIdResponseDto {
    @Schema(description = "찾고자 하는 유저의 고유 식별 아이디", example = "1")
    private final Long id;

    @Schema(description = "찾고자 하는 유저의 아이디", example = "user123")
    private final String loginId;
}
