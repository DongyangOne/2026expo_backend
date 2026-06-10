package one._026expo_backend.quiz.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.redis.QuizListSessionDto;
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
     *   "currentIndex": 0
     * }
     *
     * @param userId 현재 로그인한 사용자 id
     * @param quizIds 이번 퀴즈에서 풀 문제 id 목록
     * @return 프론트에게 전달할 sessionId
     */
    public String save(Long userId, List<Long> quizIds) {
        // 프론트가 이후 요청에서 사용할 퀴즈 세션 id를 랜덤 생성합니다.
        String sessionId = UUID.randomUUID().toString();

        QuizListSessionDto session = new QuizListSessionDto(sessionId, quizIds, 0, false);

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
     * 현재 퀴즈 위치 갱신
     *
     * 사용자가 현재 문제를 맞히거나 틀린 뒤,
     * 다음 문제로 넘어갈 때 currentIndex를 1 증가시켜 저장합니다.
     *
     * 예:
     * 기존 currentIndex = 0
     * 다음 currentIndex = 1
     */
    public void updateCurrentIndex(Long userId, QuizListSessionDto session, Integer currentIndex) {
        QuizListSessionDto updatedSession = new QuizListSessionDto(
                session.getSessionId(),
                session.getQuizIds(),
                currentIndex,
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
    public void complete(Long userId, QuizListSessionDto session, Integer currentIndex) {
        QuizListSessionDto completedSession = new QuizListSessionDto(
                session.getSessionId(),
                session.getQuizIds(),
                currentIndex,
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
     * Redis key 생성 메서드
     */
    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}