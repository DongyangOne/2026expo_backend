package one._026expo_backend.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin")
public class Admin {
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}