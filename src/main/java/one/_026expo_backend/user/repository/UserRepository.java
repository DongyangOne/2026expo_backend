package one._026expo_backend.user.repository;

import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {

    boolean existsByLoginId(String loginId);

	boolean existsByEmail(String email);

	boolean existsByLoginIdAndEmail(String loginId, String email);

	Optional<Users> findByLoginIdAndIsDeleted(String loginId, UseYnEnum isDeleted);

    Optional<Users> findByIdAndIsDeletedAndDeletedAtIsNull(Long id, UseYnEnum isDeleted);

    Optional<Users> findBySocialTypeAndSocialProviderId(SocialType socialType, String socialProviderId);

    Optional<Users> findByEmail(String email);

	Optional<Users> findByLoginIdAndEmail(String loginId, String email);

	Optional<Users> findByLoginId(String loginId);
}
