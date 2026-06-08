package one._026expo_backend.quiz.repository;

import one._026expo_backend.quiz.domain.QuizRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizRecordRepository extends JpaRepository<QuizRecord, Long> {
}
