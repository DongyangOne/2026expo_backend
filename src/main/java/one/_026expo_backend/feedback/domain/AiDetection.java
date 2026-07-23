package one._026expo_backend.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.DetectionProcessStatus;
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

    @Column(name = "client_id", nullable = false, length = 36)
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
}
