package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.character.domain.Character;
import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.character.enums.LevelPolicy;
import one._026expo_backend.character.repository.CharacterRepository;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.enums.DetectionProcessStatus;
import one._026expo_backend.feedback.enums.DetectionStatus;
import one._026expo_backend.feedback.enums.WasteClassificationStatus;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private static final int AI_CLASSIFICATION_SUCCESS_EXP = 50;

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AiDetectionRepository aiDetectionRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterRepository characterRepository;
    private final FeedbackDetailService feedbackDetailService;

    public List<RecyclingLogInfo> getRecentRecyclingLogs(long userId) {
        Pageable pageable = PageRequest.of(0, 10);

        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(feedback -> RecyclingLogInfo.of(
                        feedback.getWasteType(),
                        feedback.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public PageResponseDto<FeedbackListResponseDto> getFeedbackList(Long userId, PageRequestDto pageRequestDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());
        Page<Feedback> feedbackPage = feedbackRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);
        Page<FeedbackListResponseDto> dtoPage = feedbackPage.map(FeedbackListResponseDto::from);

        return PageResponseDto.from(dtoPage);
    }

    @Transactional
    public void saveAiFeedback(AiFeedbackRequestDto dto) {
        log.info("[AI_CALLBACK_PROCESS_START] clientId={}, rawStatus={}", dto.getClientId(), dto.getStatus());

        AiDetection detection = aiDetectionRepository
                .findByClientId(dto.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DETECTION_NOT_FOUND));

        if (detection.getStatus() == DetectionProcessStatus.COMPLETED
                && detection.getClassificationStatus() != null) {
            log.info(
                    "[AI_CALLBACK_DUPLICATE_IGNORED] clientId={}, savedStatus={}",
                    dto.getClientId(),
                    detection.getClassificationStatus()
            );
            return;
        }

        Users user = detection.getUser();
        WasteClassificationStatus classificationStatus = toClassificationStatus(dto.getStatus());
        WasteType wasteType = extractWasteType(dto);
        String guidanceCode = extractGuidanceCode(dto);
        String message = createTabletMessage(dto);
        String guideVideoUrl = createGuideVideoUrl(classificationStatus, wasteType, guidanceCode);

        Integer earnedExp = null;
        Integer totalExp = null;
        Integer level = null;
        Long userCharacterId = null;
        Long characterId = null;
        String characterName = null;
        Integer evolutionStage = null;
        Integer beforeLevel = null;
        Integer beforeExp = null;
        Integer currentLevel = null;
        Integer currentExp = null;
        Integer maxExp = null;

        if (classificationStatus == WasteClassificationStatus.ALLOWED) {
            earnedExp = AI_CLASSIFICATION_SUCCESS_EXP;
            UserCharacter userCharacter = userCharacterRepository.findFirstByUser(user)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND));

            beforeLevel = userCharacter.getCurrentLevel();
            beforeExp = userCharacter.getCurrentExp();

            userCharacter.addExp(earnedExp);
            syncCharacterWithLevel(userCharacter);

            userCharacterId = userCharacter.getUserCharacterId();
            Character character = userCharacter.getCharacter();
            characterId = character.getCharacterId();
            characterName = character.getCharacterName();
            evolutionStage = character.getEvolutionStage();
            currentLevel = userCharacter.getCurrentLevel();
            currentExp = userCharacter.getCurrentExp();
            maxExp = LevelPolicy.getMaxExpForLevel(currentLevel);
            totalExp = calculateTotalExp(currentLevel, currentExp);
            level = currentLevel;
        }

        detection.completeWithResult(
                classificationStatus,
                wasteType,
                message,
                guidanceCode,
                guideVideoUrl,
                level,
                earnedExp,
                totalExp,
                userCharacterId,
                characterId,
                characterName,
                evolutionStage,
                beforeLevel,
                beforeExp,
                currentLevel,
                currentExp,
                maxExp,
                dto.getImageUrl()
        );

        saveFeedbackIfDetected(user, classificationStatus, wasteType, guidanceCode, message);

        log.info(
                "[AI_CALLBACK_RESULT_SAVED] clientId={}, userId={}, status={}, wasteType={}, guidanceCode={}, guideVideoUrlPresent={}, earnedExp={}, level={}",
                dto.getClientId(),
                user.getId(),
                classificationStatus,
                wasteType,
                guidanceCode,
                StringUtils.hasText(guideVideoUrl),
                earnedExp,
                level
        );
    }

    private void syncCharacterWithLevel(UserCharacter userCharacter) {
        Character character = characterRepository
                .findFirstByEvolutionLevelLessThanEqualOrderByEvolutionLevelDesc(userCharacter.getCurrentLevel())
                .orElse(userCharacter.getCharacter());

        userCharacter.changeCharacter(character);
    }

    private int calculateTotalExp(int currentLevel, int currentExp) {
        int totalExp = currentExp;
        for (int level = 0; level < currentLevel; level++) {
            totalExp += LevelPolicy.getMaxExpForLevel(level);
        }
        return totalExp;
    }

    private void saveFeedbackIfDetected(
            Users user,
            WasteClassificationStatus classificationStatus,
            WasteType wasteType,
            String guidanceCode,
            String message
    ) {
        if (classificationStatus == WasteClassificationStatus.NOT_DETECTED || wasteType == null) {
            return;
        }

        Feedback feedback = Feedback.builder()
                .user(user)
                .wasteType(wasteType)
                .isFailed(classificationStatus == WasteClassificationStatus.ALLOWED ? UseYnEnum.N : UseYnEnum.Y)
                .feedbackText(classificationStatus == WasteClassificationStatus.ALLOWED ? null : message)
                .guidanceCode(guidanceCode)
                .build();

        feedbackRepository.save(feedback);
    }

    private WasteClassificationStatus toClassificationStatus(DetectionStatus status) {
        return switch (status) {
            case ALLOWED -> WasteClassificationStatus.ALLOWED;
            case REJECTED -> WasteClassificationStatus.REJECTED;
            case GENERAL_WASTE -> WasteClassificationStatus.GENERAL_WASTE;
            case NOT_DETECTED -> WasteClassificationStatus.NOT_DETECTED;
        };
    }

    private WasteType extractWasteType(AiFeedbackRequestDto dto) {
        if (dto.getClassification() == null
                || !StringUtils.hasText(dto.getClassification().getClassName())) {
            return null;
        }

        return convertWasteType(dto.getClassification().getClassName());
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

    private String extractGuidanceCode(AiFeedbackRequestDto dto) {
        if (dto.getStatus() == DetectionStatus.REJECTED
                && dto.getRejection() != null
                && StringUtils.hasText(dto.getRejection().getCode())) {
            return dto.getRejection().getCode();
        }

        if (dto.getStatus() == DetectionStatus.GENERAL_WASTE
                && dto.getGeneral() != null
                && StringUtils.hasText(dto.getGeneral().getCode())) {
            return dto.getGeneral().getCode();
        }

        String guidanceCode = dto.getGuidance() == null ? null
                : dto.getGuidance().stream()
                .map(AiFeedbackRequestDto.GuidanceDto::getCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        if (StringUtils.hasText(guidanceCode)) {
            return guidanceCode;
        }

        return null;
    }

    private String createGuideVideoUrl(
            WasteClassificationStatus status,
            WasteType wasteType,
            String guidanceCode
    ) {
        if (status == WasteClassificationStatus.ALLOWED
                || status == WasteClassificationStatus.NOT_DETECTED) {
            return null;
        }

        return feedbackDetailService.createFeedbackVideoUrl(wasteType, guidanceCode);
    }

    private String createTabletMessage(AiFeedbackRequestDto dto) {
        if (dto.getStatus() == DetectionStatus.ALLOWED) {
            return "분리배출 성공!";
        }

        String feedbackText = createFeedbackText(dto);
        if (StringUtils.hasText(feedbackText)) {
            return feedbackText;
        }

        if (dto.getStatus() == DetectionStatus.GENERAL_WASTE) {
            return "일반쓰레기입니다.";
        }

        if (dto.getStatus() == DetectionStatus.NOT_DETECTED) {
            return "인식에 실패했어요.";
        }

        return "분리배출을 다시 시도해 주세요.";
    }

    private String createFeedbackText(AiFeedbackRequestDto dto) {
        if (dto.getStatus() == DetectionStatus.ALLOWED) {
            return null;
        }

        if (dto.getStatus() == DetectionStatus.REJECTED
                && dto.getRejection() != null
                && StringUtils.hasText(dto.getRejection().getMessage())) {
            return dto.getRejection().getMessage();
        }

        if (dto.getStatus() == DetectionStatus.GENERAL_WASTE
                && dto.getGeneral() != null
                && StringUtils.hasText(dto.getGeneral().getMessage())) {
            return dto.getGeneral().getMessage();
        }

        if (dto.getGuidance() != null && !dto.getGuidance().isEmpty()) {
            String guidanceText = dto.getGuidance().stream()
                    .map(AiFeedbackRequestDto.GuidanceDto::getMessage)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n"));

            if (StringUtils.hasText(guidanceText)) {
                return guidanceText;
            }
        }

        return null;
    }
}
