package one._026expo_backend.quiz.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.redis.QuizListSessionDto;
import one._026expo_backend.quiz.dto.redis.RetryQuizSessionDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 퀴즈 진행 상태를 Redis에 저장하고 조회하는 Repository
 *
 * 기존 JPA Repository는 DB 테이블을 다루지만,
 * 이 클래스는 Redis를 저장소처럼 사용합니다.
 */
@Repository
@RequiredArgsConstructor
public class QuizSessionRedisRepository {
    //redis 세션 유지 기간(10분)
    private static final Duration QUIZ_SESSION_TTL = Duration.ofMinutes(10);

    //quiz 세션 데이터용 key정의
    private static final String KEY_PREFIX = "quiz:session:";
    private static final String RETRY_KEY_PREFIX = "quiz:retry-session:";
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 퀴즈 세션 생성
     *
     * startQuiz API에서 랜덤 퀴즈 id 리스트를 만든 뒤 호출합니다.
     *
     * Redis에는 이런 JSON이 저장됩니다.
     * {
     *   "quizIds": [3, 8, 1, 10, 5],
     *   "nextIndex": 0
     * }
     *
     * @param userId 현재 로그인한 사용자 id
     * @param quizIds 이번 퀴즈에서 풀 문제 id 목록
     * @return 프론트에게 전달할 sessionId
     */
    public String save(Long userId, List<Long> quizIds) {
        // 프론트가 이후 요청에서 사용할 퀴즈 세션 id를 랜덤 생성합니다.
        String sessionId = UUID.randomUUID().toString();

        QuizListSessionDto session = new QuizListSessionDto(sessionId, quizIds, 1, false);

        try {
            String value = objectMapper.writeValueAsString(session);

            // userId 기준으로 하나의 세션만 저장
            redisTemplate.opsForValue().set(key(userId), value, QUIZ_SESSION_TTL);

            return sessionId;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_SAVE_FAILED);
        }
    }

    /**
     * 퀴즈 세션 조회
     *
     * nextQuiz API에서 sessionId를 받아 Redis에 저장된 진행 상태를 찾을 때 사용합니다.
     *
     * @param userId 현재 로그인한 사용자 id
     * @param sessionId startQuiz에서 발급한 퀴즈 세션 id
     * @return Redis에 저장된 퀴즈 진행 상태
     */
    public QuizListSessionDto find(Long userId, String sessionId) {
        //key를 통해 value값이 있는지 조회
        String value = redisTemplate.opsForValue().get(key(userId));

        //세션 값이 없을 때 예외처리
        if (value == null) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_NOT_FOUND);
        }

        try {
            QuizListSessionDto session = objectMapper.readValue(value, QuizListSessionDto.class);

            if (!session.getSessionId().equals(sessionId)) {
                throw new BusinessException(ErrorCode.INVALID_QUIZ_SESSION);
            }

            // 조회 성공 시, dto로 변환한 값을 반환
            return session;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_READ_FAILED);
        }
    }

    /**
     * 다음 퀴즈 위치 갱신
     *
     * 사용자가 현재 문제를 맞히거나 틀린 뒤,
     * 다음에 출제할 quizIds의 인덱스를 저장합니다.
     *
     * 예:
     * 기존 nextIndex = 1
     * 다음 nextIndex = 2
     */
    public void updateNextIndex(Long userId, QuizListSessionDto session, Integer nextIndex) {
        QuizListSessionDto updatedSession = new QuizListSessionDto(
                session.getSessionId(),
                session.getQuizIds(),
                nextIndex,
                false
        );

        try {
            String value = objectMapper.writeValueAsString(updatedSession);
            // 같은 key에 새로운 value값으로 갱신
            redisTemplate.opsForValue().set(key(userId), value, QUIZ_SESSION_TTL);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_UPDATE_FAILED);
        }
    }

    /**
     * 퀴즈 완료 처리
     */
    public void complete(Long userId, QuizListSessionDto session, Integer nextIndex) {
        QuizListSessionDto completedSession = new QuizListSessionDto(
                session.getSessionId(),
                session.getQuizIds(),
                nextIndex,
                true
        );


        try {
            String value = objectMapper.writeValueAsString(completedSession);
            redisTemplate.opsForValue().set(key(userId), value, QUIZ_SESSION_TTL);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_COMPLETE_FAILED);
        }
    }

    /**
     * 퀴즈 세션 삭제
     *
     * 강제로 퀴즈 세션을 삭제해야 할 때 사용합니다.
     * 일반적인 퀴즈 완료 처리는 complete()를 사용합니다.
     */
    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    /**
     * 다시풀기 퀴즈 세션 생성
     *
     * 원본 quiz_records에서 가져온 오답 quiz id 목록을 Redis에 저장합니다.
     * 일반 퀴즈 세션과 충돌하지 않도록 별도 key prefix를 사용합니다.
     */
    public String saveRetry(Long userId, String originSessionId, List<Long> quizIds) {
        String sessionId = UUID.randomUUID().toString();

        // 다시풀기 결과 정산 시 원본 세션을 추적할 수 있게 originSessionId도 함께 저장합니다.
        RetryQuizSessionDto session = new RetryQuizSessionDto(
                sessionId,
                originSessionId,
                quizIds,
                1,
                false,
                0
        );

        try {
            String value = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(retryKey(userId), value, QUIZ_SESSION_TTL);

            return sessionId;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_SAVE_FAILED);
        }
    }

    /**
     * 다시풀기 퀴즈 세션 조회
     */
    public RetryQuizSessionDto findRetry(Long userId, String sessionId) {
        String value = redisTemplate.opsForValue().get(retryKey(userId));

        if (value == null) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_NOT_FOUND);
        }

        try {
            RetryQuizSessionDto session = objectMapper.readValue(value, RetryQuizSessionDto.class);

            if (!session.getSessionId().equals(sessionId)) {
                throw new BusinessException(ErrorCode.INVALID_QUIZ_SESSION);
            }

            return session;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_READ_FAILED);
        }
    }

    /**
     * 다시풀기 진행 상태 갱신
     *
     * 다시풀기는 DB에 기록하지 않으므로 정답 개수를 Redis에 임시 저장합니다.
     */
    public void updateRetryProgress(Long userId, RetryQuizSessionDto session, Integer nextIndex, Integer correctCount) {
        validateRetrySessionState(userId, session);

        RetryQuizSessionDto updatedSession = new RetryQuizSessionDto(
                session.getSessionId(),
                session.getOriginSessionId(),
                session.getQuizIds(),
                nextIndex,
                false,
                correctCount
        );

        try {
            String value = objectMapper.writeValueAsString(updatedSession);
            redisTemplate.opsForValue().set(retryKey(userId), value, QUIZ_SESSION_TTL);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_UPDATE_FAILED);
        }
    }

    /**
     * 다시풀기 완료 처리
     */
    public void completeRetry(Long userId, RetryQuizSessionDto session, Integer nextIndex, Integer correctCount) {
        validateRetrySessionState(userId, session);

        RetryQuizSessionDto completedSession = new RetryQuizSessionDto(
                session.getSessionId(),
                session.getOriginSessionId(),
                session.getQuizIds(),
                nextIndex,
                true,
                correctCount
        );

        try {
            String value = objectMapper.writeValueAsString(completedSession);
            redisTemplate.opsForValue().set(retryKey(userId), value, QUIZ_SESSION_TTL);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_COMPLETE_FAILED);
        }
    }

    /**
     * Redis에 저장된 기존 다시풀기 세션 상태 검증
     */
    private void validateRetrySessionState(Long userId, RetryQuizSessionDto expectedSession) {
        String existingValue = redisTemplate.opsForValue().get(retryKey(userId));

        if (existingValue != null) {
            try {
                RetryQuizSessionDto existingSession = objectMapper.readValue(existingValue, RetryQuizSessionDto.class);

                if (!existingSession.getSessionId().equals(expectedSession.getSessionId()) ||
                        !existingSession.getNextIndex().equals(expectedSession.getNextIndex())) {
                    throw new BusinessException(ErrorCode.QUIZ_SESSION_STATE_CONFLICT);
                }
            } catch (JsonProcessingException e) {
                // 파싱 실패 시 무시하고 진행
            }
        }
    }

    /**
     * 다시풀기 세션 삭제
     */
    public void deleteRetry(Long userId) {
        redisTemplate.delete(retryKey(userId));
    }

    /**
     * 퀴즈 결과 정산 중복 요청 방지용 락 (5초간 유지)
     */
    public boolean lockReward(String sessionId) {
        // "quiz:lock:세션아이디" 라는 키가 없을 때만 "Y"를 저장하고 5초 뒤에 소멸시킴
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent("quiz:lock:" + sessionId, "Y", Duration.ofSeconds(5))
        );
    }

    /**
     * 다시풀기 Redis key 생성 메서드
     */
    private String retryKey(Long userId) {
        return RETRY_KEY_PREFIX + userId;
    }

    /**
     * Redis key 생성 메서드
     */
    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}