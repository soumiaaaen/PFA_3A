package ma.ensias.content.document.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.content.common.security.CurrentUser;
import ma.ensias.content.document.entity.Document;
import ma.ensias.content.document.entity.DocumentAcl;
import ma.ensias.content.document.entity.DocumentStatus;
import ma.ensias.content.document.entity.DocumentVersion;
import ma.ensias.content.document.repository.DocumentAclRepository;
import ma.ensias.content.document.repository.DocumentRepository;
import ma.ensias.content.document.repository.DocumentVersionRepository;
import ma.ensias.content.document.web.dto.DocumentDtos.DocumentResponse;
import ma.ensias.content.integration.AdminAcademicClient;
import ma.ensias.content.integration.AdminAcademicViews.AssignmentView;
import ma.ensias.content.integration.AdminAcademicViews.DepartmentView;
import ma.ensias.content.integration.AdminAcademicViews.GroupView;
import ma.ensias.content.integration.AdminAcademicViews.ProgramView;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Liste des documents filtrée selon les droits de l'utilisateur connecté.
 * Les informations venant de l'administration-academic-service sont chargées
 * UNE fois par requête, puis chaque document est filtré en mémoire.
 */
@Service
@RequiredArgsConstructor
public class DocumentQueryService {

    private final DocumentRepository documents;
    private final DocumentVersionRepository versions;
    private final DocumentAclRepository acls;
    private final AdminAcademicClient adminAcademic;
    private final CurrentUser user;

    public record Filters(UUID elementId, UUID programId, UUID periodId, String category,
                          DocumentStatus status, String q) {
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Filters f) {
        Visibility v = new Visibility();
        return documents.findAll(spec(f), Sort.by(Sort.Direction.DESC, "updatedAt")).stream()
                // Les archives n'apparaissent que si on les demande explicitement
                .filter(d -> f.status() == DocumentStatus.ARCHIVE || d.getStatus() != DocumentStatus.ARCHIVE)
                .filter(v::canSee)
                .map(this::toResponse)
                .toList();
    }

    private Specification<Document> spec(Filters f) {
        List<Specification<Document>> parts = new ArrayList<>();
        if (f.elementId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("elementId"), f.elementId()));
        }
        if (f.programId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("programId"), f.programId()));
        }
        if (f.periodId() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("periodId"), f.periodId()));
        }
        if (f.category() != null && !f.category().isBlank()) {
            parts.add((r, q, cb) -> cb.equal(r.get("category").get("code"), f.category().toUpperCase()));
        }
        if (f.status() != null) {
            parts.add((r, q, cb) -> cb.equal(r.get("status"), f.status()));
        }
        if (f.q() != null && !f.q().isBlank()) {
            String like = "%" + f.q().strip().toLowerCase() + "%";
            parts.add((r, q, cb) -> cb.like(cb.lower(r.<String>get("title")), like));
        }
        return Specification.allOf(parts);
    }

    private DocumentResponse toResponse(Document d) {
        DocumentVersion current = d.getCurrentVersionId() == null ? null
                : versions.findById(d.getCurrentVersionId()).orElse(null);
        List<UUID> groups = acls.findByDocumentId(d.getId()).stream().map(DocumentAcl::getGroupId).toList();
        return DocumentResponse.from(d, current, groups);
    }

    /** Ce que l'utilisateur connecté a le droit de voir (mêmes règles que AccessPolicy). */
    private class Visibility {

        private final UUID me = user.id();
        private final boolean admin = user.hasRole("ADMIN");
        private final boolean teacher = user.hasRole("ENSEIGNANT");
        private final boolean student = user.hasRole("ETUDIANT");
        private final Set<UUID> headedPrograms;
        private final Set<UUID> departmentPrograms;
        private final Set<UUID> studentDocuments;
        private final Map<UUID, Set<UUID>> teacherElementsByPeriod = new HashMap<>();

        Visibility() {
            headedPrograms = user.hasRole("CHEF_FILIERE")
                    ? adminAcademic.programsHeadedBy(me).stream().map(ProgramView::id).collect(Collectors.toSet())
                    : Set.of();
            departmentPrograms = user.hasRole("CHEF_DEPARTEMENT")
                    ? adminAcademic.departmentsHeadedBy(me).stream()
                        .map(DepartmentView::id)
                        .flatMap(dep -> adminAcademic.programsOfDepartment(dep).stream())
                        .map(ProgramView::id).collect(Collectors.toSet())
                    : Set.of();
            if (student) {
                List<UUID> groups = adminAcademic.studentGroups(me).stream().map(GroupView::id).toList();
                studentDocuments = groups.isEmpty() ? Set.of() : new HashSet<>(acls.findDocumentIdsByGroupIds(groups));
            } else {
                studentDocuments = Set.of();
            }
        }

        boolean canSee(Document d) {
            return admin
                    || d.getOwnerUserId().equals(me)
                    || headedPrograms.contains(d.getProgramId())
                    || departmentPrograms.contains(d.getProgramId())
                    || (teacher && teacherElements(d.getPeriodId()).contains(d.getElementId()))
                    || (student && d.getStatus() == DocumentStatus.PUBLIE && studentDocuments.contains(d.getId()));
        }

        /** Éléments enseignés sur une période : un seul appel par période. */
        private Set<UUID> teacherElements(UUID periodId) {
            return teacherElementsByPeriod.computeIfAbsent(periodId, p ->
                    adminAcademic.teacherAssignments(p, me).stream()
                            .map(AssignmentView::elementId).collect(Collectors.toSet()));
        }
    }
}
