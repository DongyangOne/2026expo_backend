package one._026expo_backend.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.character.enums.LevelPolicy;
import one._026expo_backend.user.domain.Users;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
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

    public static UserCharacter create(Users user, Character character) {
        return UserCharacter.builder()
                .user(user)
                .character(character)
                .currentLevel(0)
                .currentExp(0)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 캐릭터 경험치를 증가시키고, 경험치가 해당 레벨의 요구 기준치를 넘으면 레벨을 올립니다.
     * LevelPolicy Enum을 참조하여 각 레벨에 맞는 요구 경험치를 동적으로 계산합니다.
     *
     * 예:
     * 현재 레벨 1(요구 경험치 100)이고 현재 경험치가 90일 때, 획득 경험치로 20을 받으면
     * currentExp는 10이 되고 currentLevel은 2로 증가합니다.
     * 최대 레벨(LEVEL_MAX)에 도달하면 더 이상 레벨이 오르지 않습니다.
     *
     * @param exp 이번 퀴즈 결과로 획득한 경험치
     */
    public void addExp(Integer exp) {
        this.currentExp += exp;

        // 현재 레벨의 최대 경험치를 Enum에서 가져옴
        int requiredExp = LevelPolicy.getMaxExpForLevel(this.currentLevel);

        // 경험치가 요구치를 넘으면 레벨업 진행 (요구치가 다를 수 있으므로 매번 갱신)
        while (this.currentExp >= requiredExp && this.currentLevel < LevelPolicy.LEVEL_10.getLevel()) {
            this.currentExp -= requiredExp;
            this.currentLevel += 1;
            // 레벨업 후, 다음 레벨의 요구 경험치로 갱신
            requiredExp = LevelPolicy.getMaxExpForLevel(this.currentLevel);
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void changeCharacter(Character character) {
        if (character == null) {
            return;
        }

        this.character = character;
        this.updatedAt = LocalDateTime.now();
    }
}
