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


    /**
     * 캐릭터 경험치를 증가시키고, 경험치가 레벨업 기준치를 넘으면 레벨을 올립니다.
     *
     * 예:
     * 현재 경험치가 90이고 획득 경험치가 20, 레벨업 기준치가 100이면
     * currentExp는 10이 되고 currentLevel은 1 증가합니다.
     *
     * @param exp 이번 퀴즈 결과로 획득한 경험치
     * @param maxExpPerLevel 레벨업에 필요한 경험치 기준값
     */
    public void addExp(Integer exp, Integer maxExpPerLevel) {
        this.currentExp += exp;

        while (this.currentExp >= maxExpPerLevel) {
            this.currentExp -= maxExpPerLevel;
            this.currentLevel += 1;
        }

        this.updatedAt = LocalDateTime.now();
    }
}

