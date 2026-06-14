package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.repository.FeedbackDetailRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackDetailService {
    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;

    public FeedbackDetailResponseDto getFeedbackDetail(Long userId, Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        FeedbackDetail detail = feedbackDetailRepository.findByWasteType(feedback.getWasteType())
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_DETAIL_NOT_FOUND));

        return FeedbackDetailResponseDto.of(feedback, detail);
    }
}
