package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.request.TabletClassificationRequestDto;
import one._026expo_backend.feedback.dto.response.AiClassificationResponseDto;
import one._026expo_backend.feedback.dto.response.TabletClassificationResponseDto;
import one._026expo_backend.feedback.enums.DetectionStatus;
import one._026expo_backend.feedback.enums.WasteClassificationStatus;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TabletClassificationService {

    private final AiDetectionRepository aiDetectionRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final FeedbackDetailService feedbackDetailService;
    private final AiClassificationClient aiClassificationClient;

    public TabletClassificationResponseDto getResult(String clientId) {
        return aiDetectionRepository.findByClientId(clientId)
                .map(TabletClassificationResponseDto::from)
                .orElseGet(() -> TabletClassificationResponseDto.waiting(clientId));
    }

    @Transactional
    public TabletClassificationResponseDto classify(TabletClassificationRequestDto request) {
        AiDetection detection = aiDetectionRepository.findByClientId(request.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DETECTION_NOT_FOUND));

        AiClassificationResponseDto aiResponse = aiClassificationClient.detect(request);
        ClassificationResult result = normalize(aiResponse, detection.getUser(), request.getImageUrl());

        detection.completeWithResult(
                result.status(),
                result.wasteType(),
                result.message(),
                result.guidanceCode(),
                result.guideVideoUrl(),
                result.level(),
                result.earnedExp(),
                result.imageUrl()
        );

        saveFeedbackIfDetected(detection.getUser(), result);

        return TabletClassificationResponseDto.from(detection);
    }

    private ClassificationResult normalize(AiClassificationResponseDto aiResponse, Users user, String imageUrl) {
        if (aiResponse == null || !StringUtils.hasText(aiResponse.getStatus())) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED);
        }

        DetectionStatus aiStatus = parseAiStatus(aiResponse.getStatus());
        WasteClassificationStatus status = toTabletStatus(aiStatus);

        if (status == WasteClassificationStatus.NOT_DETECTED) {
            return new ClassificationResult(
                    status,
                    null,
                    createMessage(aiResponse, aiStatus),
                    null,
                    null,
                    null,
                    null,
                    imageUrl
            );
        }

        WasteType wasteType = null;
        if (aiResponse.getClassification() != null
                && StringUtils.hasText(aiResponse.getClassification().getClassName())) {
            wasteType = convertWasteType(aiResponse.getClassification().getClassName());
        }

        String guidanceCode = extractGuidanceCode(aiResponse);
        String guideVideoUrl = status == WasteClassificationStatus.REJECTED
                ? feedbackDetailService.createFeedbackVideoUrl(wasteType, guidanceCode)
                : null;

        Integer level = status == WasteClassificationStatus.ALLOWED
                ? userCharacterRepository.findFirstByUser(user)
                .map(userCharacter -> userCharacter.getCurrentLevel())
                .orElse(null)
                : null;

        return new ClassificationResult(
                status,
                wasteType,
                createMessage(aiResponse, aiStatus),
                guidanceCode,
                guideVideoUrl,
                level,
                null,
                imageUrl
        );
    }

    private DetectionStatus parseAiStatus(String status) {
        try {
            return DetectionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.AI_SERVER_REQUEST_FAILED);
        }
    }

    private WasteClassificationStatus toTabletStatus(DetectionStatus aiStatus) {
        return switch (aiStatus) {
            case ALLOWED -> WasteClassificationStatus.ALLOWED;
            case NOT_DETECTED -> WasteClassificationStatus.NOT_DETECTED;
            case REJECTED -> WasteClassificationStatus.REJECTED;
            case GENERAL_WASTE -> WasteClassificationStatus.GENERAL_WASTE;
        };
    }

    private WasteType convertWasteType(String className) {
        if (!StringUtils.hasText(className)) {
            throw new BusinessException(ErrorCode.WASTE_TYPE_NOT_FOUND);
        }

        return switch (className.toLowerCase()) {
            case "plastic", "pet" -> WasteType.PLASTIC;
            case "can" -> WasteType.CAN;
            case "paper" -> WasteType.PAPER;
            case "vinyl" -> WasteType.VINYL;
            case "glass" -> WasteType.GLASS;
            case "battery" -> WasteType.BATTERY;
            case "fluorescent" -> WasteType.FLUORESCENT;
            case "styrofoam" -> WasteType.STYROFOAM;
            default -> throw new BusinessException(ErrorCode.WASTE_TYPE_NOT_FOUND);
        };
    }

    private String extractGuidanceCode(AiClassificationResponseDto aiResponse) {
        String guidanceCode = firstGuidanceCode(aiResponse.getGuidance());
        if (StringUtils.hasText(guidanceCode)) {
            return guidanceCode;
        }

        if (aiResponse.getRejection() != null && StringUtils.hasText(aiResponse.getRejection().getCode())) {
            return aiResponse.getRejection().getCode();
        }

        if (aiResponse.getGeneral() != null && StringUtils.hasText(aiResponse.getGeneral().getCode())) {
            return aiResponse.getGeneral().getCode();
        }

        return null;
    }

    private String firstGuidanceCode(List<AiClassificationResponseDto.GuidanceDto> guidance) {
        if (guidance == null) {
            return null;
        }

        return guidance.stream()
                .map(AiClassificationResponseDto.GuidanceDto::getCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String createMessage(AiClassificationResponseDto aiResponse, DetectionStatus aiStatus) {
        if (aiStatus == DetectionStatus.ALLOWED) {
            return "분리배출 성공!";
        }

        String guidanceMessage = collectGuidanceMessages(aiResponse.getGuidance());
        if (StringUtils.hasText(guidanceMessage)) {
            return guidanceMessage;
        }

        if (aiResponse.getRejection() != null
                && StringUtils.hasText(aiResponse.getRejection().getMessage())) {
            return aiResponse.getRejection().getMessage();
        }

        if (aiResponse.getGeneral() != null
                && StringUtils.hasText(aiResponse.getGeneral().getMessage())) {
            return aiResponse.getGeneral().getMessage();
        }

        if (aiStatus == DetectionStatus.NOT_DETECTED) {
            return "인식에 실패했어요.";
        }

        if (aiStatus == DetectionStatus.GENERAL_WASTE) {
            return "일반쓰레기입니다.";
        }

        return "분리배출을 다시 시도해 주세요.";
    }

    private String collectGuidanceMessages(List<AiClassificationResponseDto.GuidanceDto> guidance) {
        if (guidance == null || guidance.isEmpty()) {
            return null;
        }

        return guidance.stream()
                .map(AiClassificationResponseDto.GuidanceDto::getMessage)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }

    private void saveFeedbackIfDetected(
            Users user,
            ClassificationResult result
    ) {
        if (result.status() == WasteClassificationStatus.NOT_DETECTED || result.wasteType() == null) {
            return;
        }

        Feedback feedback = Feedback.builder()
                .user(user)
                .wasteType(result.wasteType())
                .guidanceCode(result.guidanceCode())
                .feedbackText(result.status() == WasteClassificationStatus.ALLOWED ? null : result.message())
                .isFailed(result.status() == WasteClassificationStatus.ALLOWED ? UseYnEnum.N : UseYnEnum.Y)
                .build();

        feedbackRepository.save(feedback);
    }

    private record ClassificationResult(
            WasteClassificationStatus status,
            WasteType wasteType,
            String message,
            String guidanceCode,
            String guideVideoUrl,
            Integer level,
            Integer earnedExp,
            String imageUrl
    ) {
    }
}
