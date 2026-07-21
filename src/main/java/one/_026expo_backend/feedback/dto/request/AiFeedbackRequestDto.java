package one._026expo_backend.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.global.enums.UseYnEnum;

@Schema(description = "AI 하드웨어 쓰레기 분류 결과 수신 요청 DTO")
@Getter
@NoArgsConstructor
public class AiFeedbackRequestDto {

    @Schema(description = "사용자 식별자(PK)", example = "1")
    @NotNull(message = "유저 ID는 필수입니다.")
    private Long userId;

    @Schema(description = "인식된 쓰레기 종류 (Enum)", example = "PET")
    @NotNull(message = "쓰레기 종류는 필수입니다.")
    private WasteType wasteType;

    @Schema(description = "올바른 분리수거 실패 여부 (Y: 실패, N: 성공)", example = "Y")
    @NotNull(message = "실패 여부(Y/N)는 필수입니다.")
    private UseYnEnum isFailed;

    @Schema(description = "실패 시 사용자에게 보여줄 피드백 메시지 (isFailed가 N일 경우 null 허용)",
            example = "페트병의 라벨을 완전히 제거한 후 버려주세요.")
    private String feedbackText;
}