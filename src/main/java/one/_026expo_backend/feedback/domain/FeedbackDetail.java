package one._026expo_backend.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.feedback.enums.WasteType;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "feedback_detail")
public class FeedbackDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_detail_id")
    private Long feedbackDetailId;

    @Enumerated(EnumType.STRING)
    @Column(name = "waste_type", nullable = false, columnDefinition = "ENUM('CAN', 'PET', 'PAPER','TRASH')")
    private WasteType wasteType;

    @Column(name = "feedback_content", nullable = false, columnDefinition = "TEXT")
    private String feedbackContent;

    @Column(name = "feedback_video_addr", nullable = false, columnDefinition = "TEXT")
    private String feedbackVideoAddr;
}
