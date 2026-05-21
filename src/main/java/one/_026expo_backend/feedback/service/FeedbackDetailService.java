package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.repository.FeedbackDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackDetailService {
    private final FeedbackDetailRepository feedbackDetailRepository;
}
