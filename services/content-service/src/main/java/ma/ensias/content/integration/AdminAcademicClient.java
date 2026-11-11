package ma.ensias.content.integration;

import ma.ensias.content.common.exception.BadRequestException;
import ma.ensias.content.integration.AdminAcademicViews.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

/**
 * Appels à l'administration-academic-service (API /internal, clé X-Internal-Key).
 * Aucun accès direct à sa base : uniquement ses API.
 */
@Component
public class AdminAcademicClient {

    private static final ParameterizedTypeReference<List<AssignmentView>> ASSIGNMENTS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<GroupView>> GROUPS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<ProgramView>> PROGRAMS = new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<DepartmentView>> DEPARTMENTS = new ParameterizedTypeReference<>() { };

    private final RestClient http;

    public AdminAcademicClient(@Value("${app.admin-academic.base-url}") String baseUrl,
                               @Value("${app.internal.api-key}") String apiKey) {
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", apiKey)
                .build();
    }

    /** L'élément, sa filière et sa période. Erreur 400 s'il n'existe pas. */
    public ElementView element(UUID elementId) {
        try {
            return http.get().uri("/internal/elements/{id}", elementId).retrieve().body(ElementView.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new BadRequestException("Élément introuvable : " + elementId);
        }
    }

    /** Affectations actives d'un enseignant pour un élément, sur une période. */
    public List<AssignmentView> assignments(UUID periodId, UUID teacherUserId, UUID elementId) {
        return http.get().uri(u -> u.path("/internal/assignments")
                        .queryParam("periodId", periodId)
                        .queryParam("teacherUserId", teacherUserId)
                        .queryParam("elementId", elementId)
                        .build())
                .retrieve().body(ASSIGNMENTS);
    }

    /** Groupes dont l'étudiant fait partie. */
    public List<GroupView> studentGroups(UUID studentUserId) {
        return http.get().uri("/internal/students/{id}/groups", studentUserId).retrieve().body(GROUPS);
    }

    /** Filières dirigées par un chef de filière. */
    public List<ProgramView> programsHeadedBy(UUID headUserId) {
        return http.get().uri(u -> u.path("/internal/programs").queryParam("headUserId", headUserId).build())
                .retrieve().body(PROGRAMS);
    }

    /** Filières d'un département. */
    public List<ProgramView> programsOfDepartment(UUID departmentId) {
        return http.get().uri(u -> u.path("/internal/programs").queryParam("departmentId", departmentId).build())
                .retrieve().body(PROGRAMS);
    }

    /** Départements dirigés par un chef de département. */
    public List<DepartmentView> departmentsHeadedBy(UUID headUserId) {
        return http.get().uri(u -> u.path("/internal/departments").queryParam("headUserId", headUserId).build())
                .retrieve().body(DEPARTMENTS);
    }
}
