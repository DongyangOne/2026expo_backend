package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.enums.WasteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackDetailRepository extends JpaRepository<FeedbackDetail,Long> {
    // 쓰레기 타입으로 상세 가이드 조회
    Optional<FeedbackDetail> findByWasteType(WasteType wasteType);
}
