package one._026expo_backend.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.user.enums.WithdrawReasonType;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "withdraw")
public class Withdraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false, columnDefinition = "ENUM('DELETE_RECORD', 'SERVICE_ERROR', 'LOW_FREQUENCY', 'ETC')")
    private WithdrawReasonType reasonType;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "withdraw_at", nullable = false)
    private LocalDateTime withdrawAt;
}
