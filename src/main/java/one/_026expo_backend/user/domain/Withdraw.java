package one._026expo_backend.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.user.enums.WithdrawReasonType;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "withdraw")
public class Withdraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, columnDefinition = "ENUM('DELETE_RECORD', 'SERVICE_ERROR', 'LOW_FREQUENCY', 'ETC')")
    private WithdrawReasonType reasonType;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "withdraw_at", nullable = false)
    private LocalDateTime withdrawAt;

    public static Withdraw create(Long userId, WithdrawReasonType reasonType, String reasonDetail) {
        return Withdraw.builder()
                .userId(userId)
                .reasonType(reasonType)
                .reasonDetail(reasonDetail)
                .withdrawAt(LocalDateTime.now())
                .build();
    }
}
