package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.Feedback;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 특정 사용자의 최근 10개의 분리수거 기록을 정렬
    List<Feedback> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);
}