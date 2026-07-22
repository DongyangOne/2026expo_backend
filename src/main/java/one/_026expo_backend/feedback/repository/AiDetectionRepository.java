package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.AiDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiDetectionRepository extends JpaRepository<AiDetection, Long> {

    Optional<AiDetection> findByClientId(String clientId);
}
