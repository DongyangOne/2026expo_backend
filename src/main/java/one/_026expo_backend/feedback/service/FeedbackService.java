package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

import one._026expo_backend.global.enums.UseYnEnum;
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
import org.springframework.util.StringUtils;

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

    public PageResponseDto<FeedbackListResponseDto> getFeedbackList(Long userId, PageRequestDto pageRequestDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());

        Page<Feedback> feedbackPage = feedbackRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        Page<FeedbackListResponseDto> dtoPage = feedbackPage.map(FeedbackListResponseDto::from);

        return PageResponseDto.from(dtoPage);
    }

    /**
     * AI 웹훅 데이터를 받아 Feedback 테이블에 즉시 저장
     */
    @Transactional
    public void saveAiFeedback(AiFeedbackRequestDto dto) {
        //실패인데 실패 사유 누락 시 에외처리
        if (dto.getIsFailed() == UseYnEnum.Y && !StringUtils.hasText(dto.getFeedbackText())) {
            throw new BusinessException(ErrorCode.MISSING_FEEDBACK_TEXT);
        }

        //성공 시 실패 사유 null 처리
        String validFeedbackText = (dto.getIsFailed() == UseYnEnum.N) ? null : dto.getFeedbackText();

        // 유저 검증
        Users user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 피드백 엔티티 생성
        Feedback feedback = Feedback.builder()
                .user(user)
                .wasteType(dto.getWasteType())
                .isFailed(dto.getIsFailed())
                .feedbackText(validFeedbackText)
                .build();

        // 저장
        feedbackRepository.save(feedback);
    }
}
