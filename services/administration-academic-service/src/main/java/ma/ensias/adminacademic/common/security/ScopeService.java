package ma.ensias.adminacademic.common.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScopeService {

    public UUID currentUserId() {
        Jwt jwt = (Jwt) authentication().getPrincipal();
        return UUID.fromString(jwt.getSubject());
    }

    public boolean hasRole(String role) {
        return authentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /** L'admin gère tout ; un chef de filière ne gère que la filière dont il est responsable. */
    public void checkCanManageProgram(UUID programHeadUserId) {
        if (hasRole("ADMIN")) {
            return;
        }
        if (hasRole("CHEF_FILIERE") && currentUserId().equals(programHeadUserId)) {
            return;
        }
        throw new AccessDeniedException("Vous ne pouvez gérer que les filières dont vous êtes responsable");
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}