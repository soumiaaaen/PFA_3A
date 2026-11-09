package ma.ensias.adminacademic.identity.web.dto;

import jakarta.validation.constraints.NotEmpty;
import ma.ensias.adminacademic.identity.entity.RoleCode;

import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<RoleCode> roles) {
}