package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;

    /**
     * 로그인한 사용자의 최근 분리수거 기록(~10개)을 반환
     *
     * @param userId 분리수거 기록을 확인하고자 하는 사용자 고유 아이디
     * @return 분리수거 기록 (최근 10개)
     */
    public List<RecyclingLogInfo> getRecentRecyclingLogs(long userId) {
        // 최근 10개까지만 조회
        Pageable pageable = PageRequest.of(0, 10);

        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(feedback -> RecyclingLogInfo.of(
                        feedback.getWasteType(),
                        feedback.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}