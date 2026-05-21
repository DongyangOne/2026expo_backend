package one._026expo_backend.quiz.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.quiz.service.QuizService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
@Tag(name = "quiz", description = "퀴즈 엔드포인트")
public class QuizController {
    private final QuizService quizService;
}
