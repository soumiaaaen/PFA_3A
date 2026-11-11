package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.integration.AdminAcademicClient;
import ma.ensias.content.integration.AdminAcademicViews.UserView;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VisibilityFactory {

    private final CurrentUser currentUser;
    private final AdminAcademicClient adminAcademic;
    private final DocumentAclRepository acls;

    /** Pour l'utilisateur connecté (rôles lus dans son JWT). */
    public Visibility forCurrentUser() {
        return new Visibility(currentUser.id(), currentUser.roles(), adminAcademic, acls);
    }

    /** Pour n'importe quel utilisateur (rôles demandés à l'administration-academic-service). Vide s'il n'existe pas ou est désactivé. */
    public Optional<Visibility> forUser(UUID userId) {
        UserView u = adminAcademic.user(userId);
        if (u == null || !u.active() || u.roles() == null) {
            return Optional.empty();
        }
        return Optional.of(new Visibility(userId, u.roles(), adminAcademic, acls));
    }
}
