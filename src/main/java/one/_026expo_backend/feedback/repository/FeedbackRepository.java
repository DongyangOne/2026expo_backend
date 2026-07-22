package one._026expo_backend.feedback.repository;

import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.user.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // 특정 유저의 피드백 목록을 최신순으로 조회
    Page<Feedback> findAllByUserOrderByCreatedAtDesc(Users user, Pageable pageable);
    // 특정 사용자의 최근 10개의 분리수거 기록을 정렬
    List<Feedback> findByUserIdOrderByCreatedAtDesc(long userId, Pageable pageable);

    /**
     * 피드백 ID와 사용자 소속을 동시에 검사하여 상세조회
     */
    Page<Feedback> findAllByUser_TeamOrderByCreatedAtDesc(String team, Pageable pageable);
}
