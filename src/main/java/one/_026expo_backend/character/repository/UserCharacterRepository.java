package one._026expo_backend.character.repository;

import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCharacterRepository extends JpaRepository<UserCharacter, Long> {
    Optional<UserCharacter> findFirstByUser(Users user);
    @Query("SELECT uc FROM UserCharacter uc JOIN FETCH uc.character WHERE uc.user.id = :userId")
    Optional<UserCharacter> findByUserIdWithCharacter(@Param("userId") Long userId);
}
