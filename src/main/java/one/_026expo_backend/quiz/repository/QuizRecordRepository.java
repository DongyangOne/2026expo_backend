package one._026expo_backend.quiz.repository;

import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
    boolean existsByUsersAndQuizAndSessionId(Users users, Quiz quiz, String sessionId);

    // 특정 유저가 푼 총 문제 수
    int countByUsersId(long userId);

    // 특정 유저가 맞춘 문제 수
    int countByUsersIdAndIsCorrect(long userId, UseYnEnum isCorrect);
}
