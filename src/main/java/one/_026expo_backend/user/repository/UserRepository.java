package one._026expo_backend.user.repository;

import one._026expo_backend.user.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long>{

	boolean existsByLoginId(String loginId);
}
