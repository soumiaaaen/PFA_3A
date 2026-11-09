package ma.ensias.adminacademic.identity.repository;

import ma.ensias.adminacademic.identity.entity.Role;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findByCodeIn(Collection<RoleCode> codes);
}