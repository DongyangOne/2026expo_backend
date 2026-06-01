package one._026expo_backend.admin.repository;

import one._026expo_backend.admin.domain.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin,Long> {

    boolean existsByAdminId(String adminId);
}
