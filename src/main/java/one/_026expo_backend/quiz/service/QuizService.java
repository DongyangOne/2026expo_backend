package one._026expo_backend.quiz.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.quiz.repository.QuizRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizRepository;
}
