package one._026expo_backend.character.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "characters")
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "character_id")
    private Long characterId;

    @Column(name = "character_name", nullable = false, length = 255)
    private String characterName;

    @Column(name = "image_url", nullable = false, length = 255)
    private String imageUrl;

    @Column(name = "evolution_stage", nullable = false)
    private Integer evolutionStage;

    @Column(name = "evolution_level")
    private Integer evolutionLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_character_id")
    private Character nextCharacter;
}