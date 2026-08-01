package one._026expo_backend.feedback.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.character.domain.Character;
import one._026expo_backend.character.repository.CharacterRepository;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.dto.response.TabletClassificationResponseDto;
import one._026expo_backend.feedback.enums.WasteClassificationStatus;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TabletClassificationService {

    private final AiDetectionRepository aiDetectionRepository;
    private final CharacterRepository characterRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url-expiry-hours}")
    private int urlExpiryHours;

    public TabletClassificationResponseDto getResult(String clientId) {
        return aiDetectionRepository.findByClientId(clientId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    log.info("[TABLET_CLASSIFICATION_RESULT_WAITING] clientId={}", clientId);
                    return TabletClassificationResponseDto.waiting(clientId);
                });
    }

    private TabletClassificationResponseDto toResponse(AiDetection detection) {
        String characterImageUrl = null;
        if (detection.getClassificationStatus() == WasteClassificationStatus.ALLOWED
                && detection.getCharacterId() != null) {
            characterImageUrl = createCharacterImageUrl(detection.getCharacterId());
        }

        TabletClassificationResponseDto response = TabletClassificationResponseDto.from(detection, characterImageUrl);
        log.info(
                "[TABLET_CLASSIFICATION_RESULT_FOUND] clientId={}, completed={}, status={}, wasteType={}, guideVideoUrlPresent={}, characterImageUrlPresent={}",
                response.getClientId(),
                response.isCompleted(),
                response.getStatus(),
                response.getWasteType(),
                response.getGuideVideoUrl() != null && !response.getGuideVideoUrl().isBlank(),
                characterImageUrl != null && !characterImageUrl.isBlank()
        );

        return response;
    }

    private String createCharacterImageUrl(Long characterId) {
        Character character = characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));

        String imageUrl = character.getImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(imageUrl)
                            .expiry(urlExpiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMAGE_URL_GENERATION_FAILED);
        }
    }
}
