package one._026expo_backend.quiz.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.redis.QuizListSessionDto;
import one._026expo_backend.quiz.dto.redis.RetryQuizSessionDto;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
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
     * 세션 ID 확인과 삭제를 트랜잭션으로 묶어(Atomic Compare-and-Delete),
     * 삭제 직전에 새로운 세션이 생성되더라도 잘못 삭제되는 것을 방지합니다.
     */
    public void delete(Long userId, String expectedSessionId) {
        String redisKey = key(userId);

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // 해당 키 감시
                operations.watch(redisKey);

                String existingValue = (String) operations.opsForValue().get(redisKey);
                if (existingValue == null) {
                    operations.unwatch();
                    return null;
                }

                try {
                    QuizListSessionDto existingSession = objectMapper.readValue(existingValue, QuizListSessionDto.class);
                    // 삭제 중 새 세션으로 덮어씌워진 경우
                    if (!existingSession.getSessionId().equals(expectedSessionId)) {
                        operations.unwatch(); // unwatch및 삭제 x
                        return null;
                    }

                    //세션의 변경이 없는 경우 : 세션 삭제 정상적으로 진행
                    operations.multi();
                    operations.delete(redisKey);
                    return operations.exec();

                } catch (JsonProcessingException e) {
                    operations.unwatch();
                    throw new BusinessException(ErrorCode.QUIZ_SESSION_READ_FAILED);
                }
            }
        });
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
        RetryQuizSessionDto updatedSession = new RetryQuizSessionDto(
                session.getSessionId(),
                session.getOriginSessionId(),
                session.getQuizIds(),
                nextIndex,
                false,
                correctCount
        );

        // 검증과 저장을 한 번의 트랜잭션(Atomic)으로 묶어서 실행
        executeAtomicRetryUpdate(userId, session, updatedSession);
    }

    /**
     * 다시풀기 완료 처리
     */
    public void completeRetry(Long userId, RetryQuizSessionDto session, Integer nextIndex, Integer correctCount) {
        RetryQuizSessionDto completedSession = new RetryQuizSessionDto(
                session.getSessionId(),
                session.getOriginSessionId(),
                session.getQuizIds(),
                nextIndex,
                true,
                correctCount
        );

        // 검증과 저장을 한 번의 트랜잭션(Atomic)으로 묶어서 실행
        executeAtomicRetryUpdate(userId, session, completedSession);
    }

    /**
     * Redis에 저장된 기존 다시풀기 세션 상태 검증
     * 상태 확인(Check)과 저장(Set)을 한 번의 트랜잭션으로 묶어 동시성 덮어쓰기 이슈 방지
     */
    private void executeAtomicRetryUpdate(Long userId, RetryQuizSessionDto expectedSession, RetryQuizSessionDto newSession) {
        String redisKey = retryKey(userId);

        List<Object> txResults = redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                // 해당 키를 감시 시작 (도중에 건드리면 트랜잭션 취소)
                operations.watch(redisKey);

                // 현재 상태 가져오기
                String existingValue = (String) operations.opsForValue().get(redisKey);
                if (existingValue == null) {
                    operations.unwatch();
                    throw new BusinessException(ErrorCode.QUIZ_SESSION_NOT_FOUND);
                }

                try {
                    RetryQuizSessionDto existingSession = objectMapper.readValue(existingValue, RetryQuizSessionDto.class);

                    // 상태 검증
                    if (!existingSession.getSessionId().equals(expectedSession.getSessionId()) ||
                            !existingSession.getNextIndex().equals(expectedSession.getNextIndex()) ||
                            !existingSession.getFinished().equals(expectedSession.getFinished())) {
                        operations.unwatch();
                        throw new BusinessException(ErrorCode.QUIZ_SESSION_STATE_CONFLICT);
                    }

                    String newValue = objectMapper.writeValueAsString(newSession);

                    // 감시 상태에서 덮어쓰기 예약
                    operations.multi();
                    operations.opsForValue().set(redisKey, newValue, QUIZ_SESSION_TTL);

                    // 실행 (다른 요청이 새치기했다면 텅 빈 리스트 반환)
                    return operations.exec();

                } catch (JsonProcessingException e) {
                    operations.unwatch();
                    throw new BusinessException(ErrorCode.QUIZ_SESSION_READ_FAILED);
                }
            }
        });

        // 결과가 비어있다면 누군가 트랜잭션 도중에 새치기했다는 의미이므로 에러 반환
        if (txResults == null || txResults.isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_STATE_CONFLICT);
        }
    }

    /**
     * 다시풀기 세션 삭제
     *
     * 세션 ID 확인과 삭제를 트랜잭션으로 묶어(Atomic Compare-and-Delete),
     * 삭제 직전에 새로운 세션이 생성되더라도 잘못 삭제되는 것을 방지합니다.
     */
    public void deleteRetry(Long userId, String expectedSessionId) {
        String redisKey = retryKey(userId);

        redisTemplate.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(RedisOperations operations) throws DataAccessException {
                operations.watch(redisKey);

                String existingValue = (String) operations.opsForValue().get(redisKey);
                if (existingValue == null) {
                    operations.unwatch();
                    return null;
                }

                try {
                    RetryQuizSessionDto existingSession = objectMapper.readValue(existingValue, RetryQuizSessionDto.class);
                    if (!existingSession.getSessionId().equals(expectedSessionId)) {
                        operations.unwatch();
                        return null;
                    }

                    operations.multi();
                    operations.delete(redisKey);
                    return operations.exec();

                } catch (JsonProcessingException e) {
                    operations.unwatch();
                    throw new BusinessException(ErrorCode.QUIZ_SESSION_READ_FAILED);
                }
            }
        });
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