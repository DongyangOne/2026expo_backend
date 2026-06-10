package one._026expo_backend.quiz.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.NextQuizResponseDto;
import one._026expo_backend.quiz.repository.QuizRecordRepository;
import one._026expo_backend.quiz.repository.QuizRepository;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuizRecordRepository quizRecordRepository;

    @Transactional
    public NextQuizResponseDto moveOnQuiz(Long userId, NextQuizRequestDto requestDto) {
        //유저 존재여부 예외처리
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //현재 문제 정보 조회 및 예외처리
        Quiz nowQuiz = quizRepository.findById(requestDto.getCurrentQuizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        //중복 처리 확인 및 예외처리
        if (quizRecordRepository.existsByUsersAndQuiz(user, nowQuiz)) {
            throw new BusinessException(ErrorCode.ALREADY_SOLVED_QUIZ);
        }

        boolean isCorrect = nowQuiz.getAnswer().equals(requestDto.getAnswer());

        Integer earnedPoint = isCorrect ? nowQuiz.getRewardPoint() : 0;

        QuizRecord quizRecord = QuizRecord.builder()
                .users(user)
                .quiz(nowQuiz)
                .selectedAnswer(requestDto.getAnswer())
                .isCorrect(isCorrect ? UseYnEnum.Y : UseYnEnum.N)
                .earnedPoint(earnedPoint)
                .answeredAt(LocalDateTime.now())
                .build();
        quizRecordRepository.save(quizRecord);

        //다음 문제 정보 조회 및 예외처리
        Quiz nextQuiz = quizRepository.findById(requestDto.getNextQuizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        return NextQuizResponseDto.of(nowQuiz, nextQuiz, isCorrect);
    }
}
