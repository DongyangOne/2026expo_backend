package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.repository.FeedbackDetailRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackDetailService {
    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackDetailResponseDto getFeedbackDetail(Long userId, Long feedbackId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        FeedbackDetail detail = feedbackDetailRepository.findByWasteType(feedback.getWasteType())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_DETAIL_NOT_FOUND));

        return FeedbackDetailResponseDto.of(feedback, detail);
    }
}
