package one._026expo_backend.quiz.repository;

import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.user.domain.Users;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
    boolean existsByUsersAndQuizAndSessionId(Users users, Quiz quiz, String sessionId);

    long countByUsersAndSessionId(Users users, String sessionId);

    long countByUsersAndSessionIdAndIsCorrect(Users users, String sessionId, UseYnEnum isCorrect);

    @Query("""
        select coalesce(sum(qr.earnedPoint), 0)
        from QuizRecord qr
        where qr.users = :user
          and qr.sessionId = :sessionId
    """)
    Integer sumEarnedPointByUsersAndSessionId(
            @Param("user") Users user,
            @Param("sessionId") String sessionId
    );

    // 특정 유저가 푼 총 문제 수
    int countByUsersId(long userId);

    // 특정 유저가 맞춘 문제 수
    int countByUsersIdAndIsCorrect(long userId, UseYnEnum isCorrect);

    // 특정 유저가 틀린 문제 중 가장 최근의 것을 반환
    Optional<QuizRecord> findFirstByUsersIdAndIsCorrectOrderByAnsweredAtDesc(long userId, UseYnEnum isCorrect);

    // 원본 퀴즈 세션에서 틀린 문제 기록만 풀이 순서대로 조회
    List<QuizRecord> findByUsersAndSessionIdAndIsCorrectOrderByAnsweredAtAscQuizRecordIdAsc(
            Users users,
            String sessionId,
            UseYnEnum isCorrect
    );

    // 로그인한 유저가 마지막으로 푼 퀴즈 세션 id를 조회합니다.
    @Query(value = """
    select qr.session_id
    from quiz_records qr
    where qr.user_id = :userId
    group by qr.session_id
    order by max(qr.answered_at) desc, max(qr.quiz_record_id) desc
    limit 1
    """, nativeQuery = true)
    Optional<String> findLatestSessionIdByUserId(@Param("userId") Long userId);

    // 해당 원본 세션이 이미 다시풀기에 사용되었는지 확인합니다.
    boolean existsByUsersAndSessionIdAndRetryUsed(Users users, String sessionId, UseYnEnum retryUsed);

    // 다시풀기 시작 시 원본 세션 기록 전체에 사용 완료 표시를 남깁니다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update QuizRecord qr
    set qr.retryUsed = :retryUsed
    where qr.users = :user
      and qr.sessionId = :sessionId
    """)
    int updateRetryUsedByUsersAndSessionId(
            @Param("user") Users user,
            @Param("sessionId") String sessionId,
            @Param("retryUsed") UseYnEnum retryUsed
    );
}
