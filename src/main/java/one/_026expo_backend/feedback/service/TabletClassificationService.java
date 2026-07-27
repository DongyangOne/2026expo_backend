package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.response.TabletClassificationResponseDto;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TabletClassificationService {

    private final AiDetectionRepository aiDetectionRepository;

    public TabletClassificationResponseDto getResult(String clientId) {
        return aiDetectionRepository.findByClientId(clientId)
                .map(TabletClassificationResponseDto::from)
                .orElseGet(() -> TabletClassificationResponseDto.waiting(clientId));
    }
}
