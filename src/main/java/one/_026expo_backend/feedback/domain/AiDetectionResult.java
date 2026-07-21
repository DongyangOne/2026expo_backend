package one._026expo_backend.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.DetectionStatus;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_detection_results")
public class AiDetectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // DB 식별용 PK

    @Column(nullable = false)
    private String clientId; // 로그인 인증값 (스웨거의 client_id 대응)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DetectionStatus status; // 쓰레기 분류값

    @Builder
    public AiDetectionResult(String clientId, DetectionStatus status) {
        this.clientId = clientId;
        this.status = status;
    }
}