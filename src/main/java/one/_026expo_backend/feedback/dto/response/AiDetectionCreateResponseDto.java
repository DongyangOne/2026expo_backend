package one._026expo_backend.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiDetectionCreateResponseDto {

    @Schema(description = "AI 검사 건을 구분하는 고유 식별자", example = "hardware-user-001")
    private String clientId;
}
