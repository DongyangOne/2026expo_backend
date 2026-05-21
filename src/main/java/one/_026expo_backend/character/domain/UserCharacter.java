package one._026expo_backend.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.user.domain.Users;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_characters")
public class UserCharacter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_character_id")
    private Long userCharacterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private Character character;

    @Column(name = "current_level", nullable = false)
    private Integer currentLevel;

    @Column(name = "current_exp", nullable = false)
    private Integer currentExp;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
