package one._026expo_backend.character.repository;

import one._026expo_backend.character.domain.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {
    Optional<Character> findFirstByEvolutionLevelLessThanEqualOrderByEvolutionLevelDesc(Integer level);
}
