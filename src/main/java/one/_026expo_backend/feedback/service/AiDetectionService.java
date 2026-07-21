package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.AiDetectionResult;
import one._026expo_backend.feedback.dto.request.AiDetectionRequestDto;
import one._026expo_backend.feedback.repository.AiDetectionResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDetectionService {

    private final AiDetectionResultRepository aiDetectionResultRepository;

    @Transactional
    public Long saveAiResult(AiDetectionRequestDto requestDto) {
        // requestDto 내부의 toEntity()를 호출하여 바로 Entity로 조립 후 저장
        AiDetectionResult savedResult = aiDetectionResultRepository.save(requestDto.toEntity());

        return savedResult.getId();
    }
}