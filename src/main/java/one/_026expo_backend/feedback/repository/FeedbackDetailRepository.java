package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.FeedbackDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackDetailRepository extends JpaRepository<FeedbackDetail,Long> {
}
