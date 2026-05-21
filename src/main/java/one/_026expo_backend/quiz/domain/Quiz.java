package one._026expo_backend.quiz.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.entity.BaseEntity;
import one._026expo_backend.quiz.enums.QuizAnswer;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quizzes")
public class Quiz extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Long quizId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer", nullable = false, columnDefinition= "ENUM('O', 'X')")
    private QuizAnswer answer;

    @Column(name = "explan", columnDefinition = "TEXT")
    private String explan;

    @Column(name = "reward_point", nullable = false)
    private Integer rewardPoint;
}