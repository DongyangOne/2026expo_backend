package one._026expo_backend.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "피드백 상세 조회 요청 DTO")
@Getter
@Setter
public class FeedbackDetailRequestDto {

    @Schema(description = "피드백 id", example = "3")
    @Positive(message = "피드백 ID는 1 이상의 양수여야 합니다.")
    private Long feedbackId;

}