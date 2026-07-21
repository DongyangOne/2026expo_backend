package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "중복체크 응답 DTO")
public class ExistsCheckResponseDto {
    @Schema(description = "중복 여부 (Y: 존재함, N: 존재하지 않음)", example = "Y")
    private UseYnEnum exists;
}
