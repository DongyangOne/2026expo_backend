package one._026expo_backend.quiz.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.dto.response.StartQuizResponseDto;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.quiz.dto.QuizListSessionDto;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.NextQuizResponseDto;
import one._026expo_backend.quiz.repository.QuizRecordRepository;
import one._026expo_backend.quiz.repository.QuizRepository;
import one._026expo_backend.quiz.repository.QuizSessionRedisRepository;
import one._026expo_backend.user.repository.UserRepository;
import one._026expo_backend.user.domain.Users;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuizSessionRedisRepository quizSessionRedisRepository;
    private final QuizRecordRepository quizRecordRepository;

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

        //session 데이터 생성 및 session id 저장
        String sessionId = quizSessionRedisRepository.save(userId, selectedQuizIds);

        return StartQuizResponseDto.of(sessionId, firstQuiz);
    }

    @Transactional
    public NextQuizResponseDto moveOnQuiz(Long userId, NextQuizRequestDto requestDto) {
        //유저 존재여부 예외처리
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Redis에서 현재 사용자의 퀴즈 진행 상태를 조회합니다.
        QuizListSessionDto session = quizSessionRedisRepository.find(userId, requestDto.getSessionId());
        List<Long> quizIds = session.getQuizIds();
        int currentIndex = session.getCurrentIndex();

        //잘못된 범위의 index값일 시 예외처리
        if (currentIndex >= quizIds.size()) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_NOT_FOUND);
        }

        // 서버 기준으로 현재 풀어야 하는 퀴즈 id
        Long expectedQuizId = quizIds.get(currentIndex);

        // 서버 기준 현재 퀴즈 id와 요청받은 현재 id가 다를 시 예외처리
        if (!expectedQuizId.equals(requestDto.getCurrentQuizId())) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_SEQUENCE);
        }

        // 현재 문제 정보 조회
        Quiz nowQuiz = quizRepository.findById(requestDto.getCurrentQuizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));


        //퀴즈 채점
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

        //다음 문제 index 정의 및 마지막 문제인지 확인
        int nextIndex = currentIndex + 1;
        boolean finished = nextIndex >= quizIds.size();

        if (finished) {//마지막 문제일 시 세션 완료 처리 후, nextId값을 null로 반환
            quizSessionRedisRepository.complete(
                    userId,
                    session,
                    currentIndex
            );
            return NextQuizResponseDto.of(nowQuiz, null, isCorrect);
        }

        Long nextQuizId = quizIds.get(nextIndex);
        Quiz nextQuiz = quizRepository.findById(nextQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        //세션 정보(value) 업데이트
        quizSessionRedisRepository.updateCurrentIndex(
                userId,
                session,
                nextIndex
        );

        return NextQuizResponseDto.of(nowQuiz, nextQuiz, isCorrect);
    }
}
