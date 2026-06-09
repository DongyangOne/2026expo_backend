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
@Table(name = "quiz_records")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_answer", nullable = false)
    private QuizAnswer selectedAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_correct", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum isCorrect;

    @Column(name = "earned_point", nullable = false)
    private Integer earnedPoint;

    @Column(name = "answered_at", nullable = false)
    private LocalDateTime answeredAt;
}
