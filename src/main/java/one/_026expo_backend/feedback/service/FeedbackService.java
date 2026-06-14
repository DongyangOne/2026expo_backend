package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.repository.FeedbackRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public PageResponseDto<FeedbackListResponseDto> getFeedbackList(Long userId, PageRequestDto pageRequestDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());

        Page<Feedback> feedbackPage = feedbackRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        Page<FeedbackListResponseDto> dtoPage = feedbackPage.map(FeedbackListResponseDto::from);

        return PageResponseDto.from(dtoPage);
    }
}
