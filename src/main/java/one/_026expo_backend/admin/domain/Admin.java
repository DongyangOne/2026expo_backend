package one._026expo_backend.admin.domain;

import jakarta.persistence.*;
import lombok.*;
import one._026expo_backend.global.entity.BaseEntity;

import java.time.LocalDateTime;

@Builder
@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin")
public class Admin extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "admin_id", nullable = false, unique = true, length = 50)
    private String adminId;

    @Column(name = "admin_password", nullable = false, length = 255)
    private String adminPassword;

    @Column(name = "team", nullable = false, length = 50)
    private String team;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "refresh_expired_at")
    private LocalDateTime refreshExpiredAt;

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshExpiredAt) {
        this.refreshToken = refreshToken;
        this.refreshExpiredAt = refreshExpiredAt;
    }
}