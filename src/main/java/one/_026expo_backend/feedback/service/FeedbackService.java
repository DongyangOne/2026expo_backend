package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import one._026expo_backend.global.enums.ErrorCode;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

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

    /**
     * 피드백 리스트 조회 로직
     *
     * @param userId 피드백 리스트를 조회하고자 하는 사용자 고유 아이디
     * @param pageRequestDto 조회하고자 하는 피드백 리스트의 페이징 정보
     * @return 피드백 리스트
     */
    public PageResponseDto<FeedbackListResponseDto> getFeedbackList(Long userId, PageRequestDto pageRequestDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());

        Page<Feedback> feedbackPage = feedbackRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        Page<FeedbackListResponseDto> dtoPage = feedbackPage.map(FeedbackListResponseDto::from);

        return PageResponseDto.from(dtoPage);
    }
}
