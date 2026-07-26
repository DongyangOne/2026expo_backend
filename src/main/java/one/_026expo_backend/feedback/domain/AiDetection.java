package one._026expo_backend.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.DetectionProcessStatus;
import one._026expo_backend.feedback.enums.WasteClassificationStatus;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.user.domain.Users;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_detection",
        uniqueConstraints = {@UniqueConstraint(name = "uk_ai_detection_client_id", columnNames = "client_id")})
public class AiDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_detection_id")
    private Long aiDetectionId;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DetectionProcessStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification_status", length = 30)
    private WasteClassificationStatus classificationStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", length = 30)
    private WasteType wasteType;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "guidance_code", length = 50)
    private String guidanceCode;

    @Column(name = "guide_video_url", columnDefinition = "TEXT")
    private String guideVideoUrl;

    @Column(name = "level")
    private Integer level;

    @Column(name = "earned_exp")
    private Integer earnedExp;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Builder
    public AiDetection(String clientId, Users user) {
        this.clientId = clientId;
        this.user = user;
        this.status = DetectionProcessStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = DetectionProcessStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void completeWithResult(
            WasteClassificationStatus classificationStatus,
            WasteType wasteType,
            String message,
            String guidanceCode,
            String guideVideoUrl,
            Integer level,
            Integer earnedExp,
            String imageUrl
    ) {
        this.classificationStatus = classificationStatus;
        this.wasteType = wasteType;
        this.message = message;
        this.guidanceCode = guidanceCode;
        this.guideVideoUrl = guideVideoUrl;
        this.level = level;
        this.earnedExp = earnedExp;
        this.imageUrl = imageUrl;
        complete();
    }
}
