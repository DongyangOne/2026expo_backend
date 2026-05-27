package one._026expo_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "중복체크 응답 DTO")
public class ExistsCheckResponseDto {
    @Schema(description = "중복 여부 (true: 존재함)")
    private boolean exists;
}
