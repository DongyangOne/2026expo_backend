package one._026expo_backend.quiz.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.dto.response.StartQuizResponseDto;
import one._026expo_backend.quiz.repository.QuizRepository;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    public StartQuizResponseDto startQuiz(Long userId, StartQuizRequestDto requestDto) {
        //유저 존재여부 예외처리
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var allQuizIds = new ArrayList<>(quizRepository.findAllQuizIds());

        //db에 퀴즈가 하나도 없을 때 예외처리
        if (allQuizIds.isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
        }
        //db에 있는 퀴즈 개수보다 유저가 요청한 개수가 더 많을 때 예외처리
        if (allQuizIds.size() < requestDto.getQuantity()) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_QUIZ);
        }

        Collections.shuffle(allQuizIds);
        var selectedQuizIds = allQuizIds.stream()
                .limit(requestDto.getQuantity())
                .toList();

        var firstQuizId = selectedQuizIds.get(0);
        var firstQuiz = quizRepository.findById(firstQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND)); //첫번재 퀴즈에 대한 db정보가 삭제된 경우 예외처리

        return StartQuizResponseDto.of(selectedQuizIds, firstQuiz);
    }
}
