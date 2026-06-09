package one._026expo_backend.user.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.entity.BaseEntity;
import one._026expo_backend.user.enums.SocialType;
import one._026expo_backend.global.enums.UseYnEnum;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "users")
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", length = 8, nullable = false)
    private String username;

    @Column(name = "login_id", length = 12)
    private String loginId;

    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "email_verified", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum emailVerified = UseYnEnum.N;

    @Enumerated(EnumType.STRING)
    @Column(name="remember_me", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum rememberMe = UseYnEnum.N;

    @Enumerated(EnumType.STRING)
    @Column(name = "terms_agreed", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum termsAgreed = UseYnEnum.N;

    @Column(name = "social_provider_id", length = 255)
    private String socialProviderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, columnDefinition = "ENUM('GOOGLE', 'KAKAO', 'NAVER', 'LOCAL')")
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    @Column(name = "is_deleted", nullable = false, columnDefinition = "ENUM('Y','N')")
    private UseYnEnum isDeleted = UseYnEnum.N;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "refresh_token", length = 512)
    private String refreshToken;

    @Column(name = "refresh_expired_at")
    private LocalDateTime refreshExpiredAt;

    public void updateRememberMe(UseYnEnum rememberMe) {
        this.rememberMe = rememberMe;
    }

    public void updateRefreshToken(String refreshToken, LocalDateTime refreshExpiredAt) {
        this.refreshToken = refreshToken;
        this.refreshExpiredAt = refreshExpiredAt;
    }
}
