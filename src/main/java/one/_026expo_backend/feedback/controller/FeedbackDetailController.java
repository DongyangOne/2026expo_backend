package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.service.FeedbackDetailService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedbackDetail")
@RequiredArgsConstructor
@Tag(name = "feedbackDetail", description = "피드백 디테일 엔드포인트")
public class FeedbackDetailController {
    private final FeedbackDetailService feedbackDetailService;
}
