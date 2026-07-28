package one._026expo_backend.feedback.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
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
    private Integer totalExp;
    private Long userCharacterId;
    private Long characterId;
    private String characterName;
    private String characterImageUrl;
    private Integer evolutionStage;
    private Integer beforeLevel;
    private Integer beforeExp;
    private Integer currentLevel;
    private Integer currentExp;
    private Boolean levelUp;
    private Integer maxExp;
    private Integer expPercent;
    private Integer remainingExp;

    public static TabletClassificationResponseDto waiting(String clientId) {
        return TabletClassificationResponseDto.builder()
                .clientId(clientId)
                .completed(false)
                .status(WasteClassificationStatus.WAITING)
                .message("쓰레기를 인식 중입니다.")
                .build();
    }

    public static TabletClassificationResponseDto from(AiDetection detection, String characterImageUrl) {
        if (detection.getStatus() != DetectionProcessStatus.COMPLETED
                || detection.getClassificationStatus() == null) {
            return waiting(detection.getClientId());
        }

        WasteType wasteType = detection.getWasteType();
        Integer currentExp = detection.getCurrentExp();
        Integer maxExp = detection.getMaxExp();
        Integer beforeLevel = detection.getBeforeLevel();
        Integer currentLevel = detection.getCurrentLevel();

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
                .totalExp(detection.getTotalExp())
                .userCharacterId(detection.getUserCharacterId())
                .characterId(detection.getCharacterId())
                .characterName(detection.getCharacterName())
                .characterImageUrl(characterImageUrl)
                .evolutionStage(detection.getEvolutionStage())
                .beforeLevel(beforeLevel)
                .beforeExp(detection.getBeforeExp())
                .currentLevel(currentLevel)
                .currentExp(currentExp)
                .levelUp(createLevelUp(beforeLevel, currentLevel))
                .maxExp(maxExp)
                .expPercent(currentExp == null || maxExp == null || maxExp == 0 ? null : currentExp * 100 / maxExp)
                .remainingExp(currentExp == null || maxExp == null ? null : maxExp - currentExp)
                .build();
    }

    private static Boolean createLevelUp(Integer beforeLevel, Integer currentLevel) {
        if (beforeLevel == null || currentLevel == null) {
            return null;
        }

        return currentLevel > beforeLevel;
    }
}
