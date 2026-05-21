package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.service.FeedbackService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "feedback", description = "피드백 엔드포인트")
public class FeedbackController {
    private final FeedbackService feedbackService;
}
