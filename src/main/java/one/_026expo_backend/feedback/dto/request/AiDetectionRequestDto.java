package one._026expo_backend.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.domain.AiDetectionResult;
import one._026expo_backend.feedback.enums.DetectionStatus;

@Schema(description = "AI 쓰레기 분류 결과 웹훅 요청 데이터 구조")
@Getter
@NoArgsConstructor
public class AiDetectionRequestDto {

    @Schema(description = "하드웨어가 보낸 사용자/피드백 구분 ID", example = "client_12345")
    private String clientId;

    @Schema(description = "AI가 판별한 쓰레기 분류 상태값", example = "ALLOWED")
    private DetectionStatus status;

    /**
     * Request DTO의 데이터를 바탕으로 Entity를 조립(생성)하는 메서드
     */
    public AiDetectionResult toEntity() {
        return AiDetectionResult.builder()
                .clientId(this.clientId)
                .status(this.status)
                .build();
    }
}