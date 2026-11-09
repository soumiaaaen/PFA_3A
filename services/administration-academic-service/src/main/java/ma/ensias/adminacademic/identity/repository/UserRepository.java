package ma.ensias.adminacademic.identity.repository;

import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRoles_Code(RoleCode code);
}