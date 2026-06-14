package one._026expo_backend.character.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.character.domain.Character;
import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.character.dto.response.MyCharacterResponseDto;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCharacterService {
    private final UserCharacterRepository userCharacterRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url-expiry-hours}")
    private int urlExpiryHours;

    /**
     * 로그인한 사용자의 고유 식별 아이디를 받아 해당 사용자의 레벨 별 이미지와 관련 정보를 반환
     *
     * @param userId 로그인한 사용자의 고유 식별 아이디
     */
    public MyCharacterResponseDto getMyCharacter(Long userId) {
        UserCharacter userCharacter = userCharacterRepository.findByUserIdWithCharacter(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND));

        // 내부 캐릭터 정보가 null인 경우
        Character currentCharacter = userCharacter.getCharacter();
        if (currentCharacter == null) {
            throw new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND);
        }

        // MinIO에 저장된 이미지 주소
        String imageUrl = getMinioImageUrl(currentCharacter.getImageUrl());

        return MyCharacterResponseDto.of(
                currentCharacter.getCharacterId(),
                currentCharacter.getCharacterName(),
                imageUrl,
                currentCharacter.getEvolutionStage()
        );
    }

    /**
     * 테이블에 저장된 이미지 주소를 받아 MinIO Presigned URL을 반환
     *
     * @param imageUrl user_character 테이블에 저장된 이미지 주소
     * @return MinIO Presigned URL
     */
    private String getMinioImageUrl(String imageUrl) {
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
            log.error("MinIO 이미지 주소 생성 실패 - 파일 경로: {}, 이유: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}
