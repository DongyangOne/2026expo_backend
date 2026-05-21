package one._026expo_backend.character.repository;

import one._026expo_backend.character.domain.UserCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
}
