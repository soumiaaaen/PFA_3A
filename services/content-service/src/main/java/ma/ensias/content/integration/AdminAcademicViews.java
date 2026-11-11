package ma.ensias.content.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** Réponses des endpoints /internal de l'administration-academic-service (voir son INTEGRATION.md). */
public final class AdminAcademicViews {

    private AdminAcademicViews() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserView(UUID id, String email, String firstName, String lastName,
                           boolean active, Set<String> roles) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PeriodView(UUID id, Integer number, UUID semesterId, Integer semesterNumber,
                             UUID academicYearId, LocalDate startDate, LocalDate endDate) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ElementView(UUID id, String code, String name, UUID moduleId, String moduleCode,
                              UUID programId, PeriodView period) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssignmentView(UUID id, UUID elementId, UUID teacherUserId, UUID groupId,
                                 UUID periodId, String activityType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GroupView(UUID id, String name, Integer studyLevel, UUID programId,
                            UUID departmentId, UUID academicYearId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProgramView(UUID id, String code, String name, UUID departmentId, UUID headUserId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DepartmentView(UUID id, String code, String name, UUID headUserId) {
    }
}
