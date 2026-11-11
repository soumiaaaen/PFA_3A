package ma.ensias.content.document.service;

import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.integration.AdminAcademicClient;
import ma.ensias.content.integration.AdminAcademicViews.AssignmentView;
import ma.ensias.content.integration.AdminAcademicViews.DepartmentView;
import ma.ensias.content.integration.AdminAcademicViews.GroupView;
import ma.ensias.content.integration.AdminAcademicViews.ProgramView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Ce qu'UN utilisateur donné a le droit de voir (mêmes règles que AccessPolicy, conception §2.3).
 * Les informations de l'administration-academic-service sont chargées une seule fois,
 * puis chaque document est testé en mémoire. Sert à la liste des documents (utilisateur connecté)
 * et à l'API interne de l'IA (n'importe quel utilisateur).
 */
public class Visibility {

    private final AdminAcademicClient adminAcademic;
    private final UUID userId;
    private final boolean admin;
    private final boolean teacher;
    private final boolean student;
    private final Set<UUID> headedPrograms;
    private final Set<UUID> departmentPrograms;
    private final Set<UUID> studentDocuments;
    private final Map<UUID, Set<UUID>> teacherElementsByPeriod = new HashMap<>();

    Visibility(UUID userId, Set<String> roles, AdminAcademicClient adminAcademic, DocumentAclRepository acls) {
        this.adminAcademic = adminAcademic;
        this.userId = userId;
        this.admin = roles.contains("ADMIN");
        this.teacher = roles.contains("ENSEIGNANT");
        this.student = roles.contains("ETUDIANT");
        this.headedPrograms = roles.contains("CHEF_FILIERE")
                ? adminAcademic.programsHeadedBy(userId).stream().map(ProgramView::id).collect(Collectors.toSet())
                : Set.of();
        this.departmentPrograms = roles.contains("CHEF_DEPARTEMENT")
                ? adminAcademic.departmentsHeadedBy(userId).stream()
                    .map(DepartmentView::id)
                    .flatMap(dep -> adminAcademic.programsOfDepartment(dep).stream())
                    .map(ProgramView::id).collect(Collectors.toSet())
                : Set.of();
        if (student) {
            List<UUID> groups = adminAcademic.studentGroups(userId).stream().map(GroupView::id).toList();
            this.studentDocuments = groups.isEmpty() ? Set.of() : new HashSet<>(acls.findDocumentIdsByGroupIds(groups));
        } else {
            this.studentDocuments = Set.of();
        }
    }

    public boolean canSee(Document d) {
        return admin
                || d.getOwnerUserId().equals(userId)
                || headedPrograms.contains(d.getProgramId())
                || departmentPrograms.contains(d.getProgramId())
                || (teacher && teacherElements(d.getPeriodId()).contains(d.getElementId()))
                || (student && d.getStatus() == DocumentStatus.PUBLIE && studentDocuments.contains(d.getId()));
    }

    /** Éléments enseignés sur une période : un seul appel par période. */
    private Set<UUID> teacherElements(UUID periodId) {
        return teacherElementsByPeriod.computeIfAbsent(periodId, p ->
                adminAcademic.teacherAssignments(p, userId).stream()
                        .map(AssignmentView::elementId).collect(Collectors.toSet()));
    }
}
