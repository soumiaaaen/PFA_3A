package ma.ensias.content.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Informations sur l'utilisateur connecté, lues dans son JWT. */
@Component
public class CurrentUser {

    public UUID id() {
        return UUID.fromString(jwt().getSubject());
    }

    public String email() {
        return jwt().getClaimAsString("email");
    }

    /** Rôles sans le préfixe ROLE_ : ADMIN, CHEF_DEPARTEMENT, CHEF_FILIERE, ENSEIGNANT, ETUDIANT. */
    public Set<String> roles() {
        return authentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .collect(Collectors.toSet());
    }

    public boolean hasRole(String role) {
        return roles().contains(role);
    }

    private Jwt jwt() {
        return (Jwt) authentication().getPrincipal();
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
