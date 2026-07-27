package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.enums.WasteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeedbackDetailRepository extends JpaRepository<FeedbackDetail,Long> {
    // 쓰레기 종류(CAN, PET 등)로 사전 데이터 조회
    Optional<FeedbackDetail> findByWasteTypeAndGuidanceCode(WasteType wasteType, String guidanceCode);

    Optional<FeedbackDetail> findFirstByWasteTypeAndGuidanceCodeIsNull(WasteType wasteType);

    Optional<FeedbackDetail> findFirstByWasteTypeOrderByFeedbackDetailIdAsc(WasteType wasteType);

    Optional<FeedbackDetail> findFirstByGuidanceCodeOrderByFeedbackDetailIdAsc(String guidanceCode);

    Optional<FeedbackDetail> findFirstByGuidanceCodeIsNullOrderByFeedbackDetailIdAsc();

    Optional<FeedbackDetail> findFirstByFeedbackVideoAddrStartingWithOrderByFeedbackDetailIdAsc(String feedbackVideoAddr);

    Optional<FeedbackDetail> findFirstByOrderByFeedbackDetailIdAsc();
}
