package one._026expo_backend.quiz.domain;

import jakarta.persistence.*;
import lombok.*;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.enums.QuizAnswer;
import one._026expo_backend.user.domain.Users;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_records", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_quiz_record",
                columnNames = {"user_id", "session_id", "quiz_id"}
        )}
)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QuizRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_record_id")
    private Long quizRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    // 같은 퀴즈 세션 안에서 중복 제출 여부를 구분하기 위한 세션 id
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_answer", nullable = false)
    private QuizAnswer selectedAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_correct", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum isCorrect;

    @Column(name = "earned_point", nullable = false)
    private Integer earnedPoint;

    // 원본 세션의 다시풀기 경험치가 이미 지급되었는지 표시합니다.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "retry_reward_claimed", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum retryRewardClaimed = UseYnEnum.N;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
