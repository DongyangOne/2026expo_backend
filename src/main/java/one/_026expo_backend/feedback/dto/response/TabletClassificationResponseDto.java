package one._026expo_backend.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.enums.DetectionProcessStatus;
import one._026expo_backend.feedback.enums.WasteClassificationStatus;
import one._026expo_backend.feedback.enums.WasteType;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "태블릿 AI 분류 결과 응답 DTO")
public class TabletClassificationResponseDto {

    private String clientId;
    private boolean completed;
    private WasteClassificationStatus status;
    private WasteType wasteType;
    private String wasteTypeLabel;
    private String message;
    private String guidanceCode;
    private String guideVideoUrl;
    private Integer level;
    private Integer earnedExp;
    private String imageUrl;

    public static TabletClassificationResponseDto waiting(String clientId) {
        return TabletClassificationResponseDto.builder()
                .clientId(clientId)
                .completed(false)
                .status(WasteClassificationStatus.WAITING)
                .message("쓰레기를 인식 중입니다.")
                .build();
    }

    public static TabletClassificationResponseDto from(AiDetection detection) {
        if (detection.getStatus() != DetectionProcessStatus.COMPLETED
                || detection.getClassificationStatus() == null) {
            return waiting(detection.getClientId());
        }

        WasteType wasteType = detection.getWasteType();

        return TabletClassificationResponseDto.builder()
                .clientId(detection.getClientId())
                .completed(true)
                .status(detection.getClassificationStatus())
                .wasteType(wasteType)
                .wasteTypeLabel(wasteType == null ? null : wasteType.getDescription())
                .message(detection.getMessage())
                .guidanceCode(detection.getGuidanceCode())
                .guideVideoUrl(detection.getGuideVideoUrl())
                .level(detection.getLevel())
                .earnedExp(detection.getEarnedExp())
                .imageUrl(detection.getImageUrl())
                .build();
    }
}
