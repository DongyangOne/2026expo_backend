package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.AiDetectionResult;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiDetectionResultRepository extends JpaRepository<AiDetectionResult,Long> {
    Optional<AiDetectionResult> findByClientId(String clientId);
}
