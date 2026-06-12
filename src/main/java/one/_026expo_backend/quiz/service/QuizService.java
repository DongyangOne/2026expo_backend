package one._026expo_backend.quiz.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.request.QuizResultRequestDto;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.dto.response.QuizResultResponseDto;
import one._026expo_backend.quiz.dto.response.StartQuizResponseDto;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.quiz.dto.redis.QuizListSessionDto;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.NextQuizResponseDto;
import one._026expo_backend.quiz.repository.QuizRecordRepository;
import one._026expo_backend.quiz.repository.QuizRepository;
import one._026expo_backend.quiz.repository.QuizSessionRedisRepository;
import one._026expo_backend.user.repository.UserRepository;
import one._026expo_backend.user.domain.Users;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UserCharacterRepository userCharacterRepository;

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
        int nextIndex = session.getNextIndex();

        // startQuiz에서 첫 문제를 이미 내려줬으므로, 현재 채점할 문제는 nextIndex - 1 위치입니다.
        int currentIndex = nextIndex - 1;

        // 잘못된 범위의 index값일 시 예외처리
        if (currentIndex < 0 || currentIndex >= quizIds.size()) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_NOT_FOUND);
        }

        // 서버 기준으로 현재 풀어야 하는 퀴즈 id
        Long expectedQuizId = quizIds.get(currentIndex);

        // 서버 기준 현재 퀴즈 id와 요청받은 현재 id가 다를 시 예외처리
        if (!expectedQuizId.equals(requestDto.getCurrentQuizId())) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_SEQUENCE);
        }

        // 서버 세션 기준으로 검증된 퀴즈 id로 현재 문제를 조회합니다.
        Quiz nowQuiz = quizRepository.findById(expectedQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 같은 퀴즈 세션 안에서 이미 제출한 문제인지 확인합니다.
        if (quizRecordRepository.existsByUsersAndQuizAndSessionId(user, nowQuiz, session.getSessionId())) {
            throw new BusinessException(ErrorCode.ALREADY_SOLVED_QUIZ);
        }

        //퀴즈 채점
        boolean isCorrect = nowQuiz.getAnswer().equals(requestDto.getAnswer());

        Integer earnedPoint = isCorrect ? nowQuiz.getRewardPoint() : 0;

        QuizRecord quizRecord = QuizRecord.builder()
                .users(user)
                .quiz(nowQuiz)
                .sessionId(session.getSessionId())
                .selectedAnswer(requestDto.getAnswer())
                .isCorrect(isCorrect ? UseYnEnum.Y : UseYnEnum.N)
                .earnedPoint(earnedPoint)
                .answeredAt(LocalDateTime.now())
                .build();
        try {
            quizRecordRepository.save(quizRecord);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 세션의 같은 문제를 제출하면 DB unique 제약으로 중복 저장을 막습니다.
            throw new BusinessException(ErrorCode.ALREADY_SOLVED_QUIZ);
        }

        // 다음 요청에서 채점할 문제 위치를 한 칸 앞으로 이동합니다.
        int updatedNextIndex = nextIndex + 1;
        boolean finished = updatedNextIndex > quizIds.size();

        if (finished) { // 마지막 문제까지 풀이 완료 시, nextQuiz는 null로 반환합니다.
            quizSessionRedisRepository.complete(
                    userId,
                    session,
                    quizIds.size()
            );
            return NextQuizResponseDto.of(nowQuiz, null, isCorrect);
        }

        Long nextQuizId = quizIds.get(nextIndex);
        Quiz nextQuiz = quizRepository.findById(nextQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        //세션 정보(value) 업데이트
        quizSessionRedisRepository.updateNextIndex(
                userId,
                session,
                updatedNextIndex
        );

        return NextQuizResponseDto.of(nowQuiz, nextQuiz, isCorrect);
    }

    @Transactional
    public QuizResultResponseDto resultQuiz(Long userId, QuizResultRequestDto requestDto){
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuizListSessionDto session = quizSessionRedisRepository.find(userId, requestDto.getSessionId());

        if (!Boolean.TRUE.equals(session.getFinished())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FINISHED);
        }

        int totalCount = session.getQuizIds().size();

        long recordCount = quizRecordRepository.countByUsersAndSessionId(
                user,
                requestDto.getSessionId()
        );

        if (recordCount != totalCount) {
            throw new BusinessException(ErrorCode.QUIZ_RESULT_RECORD_NOT_MATCHED);
        }

        int correctCount = (int) quizRecordRepository.countByUsersAndSessionIdAndIsCorrect(
                user,
                requestDto.getSessionId(),
                UseYnEnum.Y
        );

        int earnedExp = correctCount * EXP_PER_CORRECT_ANSWER;

        UserCharacter userCharacter = userCharacterRepository.findFirstByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND));

        userCharacter.addExp(earnedExp, MAX_EXP_PER_LEVEL);

        quizSessionRedisRepository.delete(userId);

        String resultMessage = QuizResultMessage.pick(correctCount, totalCount);

        return QuizResultResponseDto.of(
                totalCount,
                correctCount,
                earnedExp,
                resultMessage,
                userCharacter.getUserCharacterId(),
                userCharacter.getCurrentLevel(),
                userCharacter.getCurrentExp(),
                MAX_EXP_PER_LEVEL
        );
    }




}
