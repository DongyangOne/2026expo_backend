package one._026expo_backend.user.repository;

import one._026expo_backend.user.domain.Withdraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WithdrawRepository extends JpaRepository<Withdraw, Long> {
}
