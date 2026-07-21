package one._026expo_backend.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.user.domain.Users;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedback")
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", nullable = false, columnDefinition = "ENUM('CAN', 'PET', 'PAPER','TRASH')")
    private WasteType wasteType;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_failed", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum isFailed = UseYnEnum.N;

    // AI가 판별한 실패 사유 (예: "캔에 음식물이 들어있었다")
    @Column(name = "feedback_text", columnDefinition = "TEXT")
    private String feedbackText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Feedback(Users user, WasteType wasteType, UseYnEnum isFailed, String feedbackText) {
        this.user = user;
        this.wasteType = wasteType;
        this.isFailed = isFailed;
        this.feedbackText = feedbackText;
        this.createdAt = LocalDateTime.now();
    }
}