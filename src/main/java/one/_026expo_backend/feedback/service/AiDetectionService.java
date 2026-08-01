package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.dto.response.AiDetectionCreateResponseDto;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiDetectionService {

    private final UserRepository userRepository;
    private final AiDetectionRepository aiDetectionRepository;

    @Transactional
    public AiDetectionCreateResponseDto createDetection(Long userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String clientId = UUID.randomUUID().toString();

        AiDetection detection = AiDetection.builder()
                .clientId(clientId)
                .user(user)
                .build();

        aiDetectionRepository.save(detection);
        log.info("[AI_DETECTION_START_CREATED] userId={}, clientId={}", userId, clientId);

        return new AiDetectionCreateResponseDto(clientId);
    }
}
