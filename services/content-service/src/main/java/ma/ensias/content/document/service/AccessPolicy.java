package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.integration.AdminAcademicClient;
import ma.ensias.content.integration.AdminAcademicViews.GroupView;
import ma.ensias.content.integration.AdminAcademicViews.ProgramView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Règles d'accès aux documents (conception §2.3). Toutes les décisions sont prises ici,
 * côté serveur, à chaque requête.
 */
@Component
@RequiredArgsConstructor
public class AccessPolicy {

    private final CurrentUser user;
    private final AdminAcademicClient adminAcademic;
    private final DocumentAclRepository aclRepository;

    /** L'utilisateur connecté est-il enseignant affecté à cet élément sur cette période ? */
    public boolean isAssignedTeacher(UUID elementId, UUID periodId) {
        return user.hasRole("ENSEIGNANT")
                && !adminAcademic.assignments(periodId, user.id(), elementId).isEmpty();
    }

    /** Groupes que l'enseignant connecté a pour cet élément (groupes proposés par défaut à la publication). */
    public List<UUID> assignedGroups(UUID elementId, UUID periodId) {
        return adminAcademic.assignments(periodId, user.id(), elementId).stream()
                .map(a -> a.groupId()).distinct().toList();
    }

    public boolean isProgramHead(UUID programId) {
        return user.hasRole("CHEF_FILIERE")
                && adminAcademic.programsHeadedBy(user.id()).stream().anyMatch(p -> p.id().equals(programId));
    }

    public boolean isDepartmentHead(UUID programId) {
        if (!user.hasRole("CHEF_DEPARTEMENT")) {
            return false;
        }
        return adminAcademic.departmentsHeadedBy(user.id()).stream()
                .flatMap(d -> adminAcademic.programsOfDepartment(d.id()).stream())
                .map(ProgramView::id)
                .anyMatch(programId::equals);
    }

    /** Déposer une version, modifier les métadonnées, choisir les groupes. */
    public boolean canEditContent(Document d) {
        return d.getOwnerUserId().equals(user.id()) || isAssignedTeacher(d.getElementId(), d.getPeriodId());
    }

    /** Publier / dépublier (décision 5 : aussi le chef de filière). */
    public boolean canPublish(Document d) {
        return canEditContent(d) || isProgramHead(d.getProgramId());
    }

    /** Archiver : auteur, chef de filière, admin. */
    public boolean canArchive(Document d) {
        return user.hasRole("ADMIN") || d.getOwnerUserId().equals(user.id()) || isProgramHead(d.getProgramId());
    }

    /** Voir les brouillons et les anciennes versions. */
    public boolean canSeeEverything(Document d) {
        return user.hasRole("ADMIN")
                || d.getOwnerUserId().equals(user.id())
                || isAssignedTeacher(d.getElementId(), d.getPeriodId())
                || isProgramHead(d.getProgramId())
                || isDepartmentHead(d.getProgramId());
    }

    /** Voir le document (et télécharger sa version courante). */
    public boolean canView(Document d) {
        if (canSeeEverything(d)) {
            return true;
        }
        if (d.getStatus() != DocumentStatus.PUBLIE || !user.hasRole("ETUDIANT")) {
            return false;
        }
        List<UUID> myGroups = adminAcademic.studentGroups(user.id()).stream().map(GroupView::id).toList();
        return !myGroups.isEmpty()
                && aclRepository.findByDocumentId(d.getId()).stream().anyMatch(a -> myGroups.contains(a.getGroupId()));
    }
}
