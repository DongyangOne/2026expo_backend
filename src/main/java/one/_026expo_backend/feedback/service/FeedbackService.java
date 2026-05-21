package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
}
