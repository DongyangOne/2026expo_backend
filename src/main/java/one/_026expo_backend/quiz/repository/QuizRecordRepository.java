package one._026expo_backend.quiz.repository;

import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
