package ma.ensias.adminacademic.integration.web;

import ma.ensias.adminacademic.academic.entity.Period;
import ma.ensias.adminacademic.academic.entity.StudentGroup;
import ma.ensias.adminacademic.assignment.entity.ActivityType;
import ma.ensias.adminacademic.assignment.entity.TeachingAssignment;
import ma.ensias.adminacademic.catalog.entity.Element;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.academic.entity.Department;
import ma.ensias.adminacademic.academic.entity.Program;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Réponses des endpoints /internal, destinées aux autres microservices. */
public final class InternalViews {

    private InternalViews() {
    }

    public record UserView(UUID id, String email, String firstName, String lastName,
                           boolean active, Set<String> roles) {
        public static UserView from(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.isActive(),
                    u.getRoles().stream().map(r -> r.getCode().name()).collect(Collectors.toSet()));
        }
    }

    public record PeriodView(UUID id, Integer number, UUID semesterId, Integer semesterNumber,
                             UUID academicYearId, LocalDate startDate, LocalDate endDate) {
        public static PeriodView from(Period p) {
            return new PeriodView(p.getId(), p.getNumber(),
                    p.getSemester().getId(), p.getSemester().getNumber(),
                    p.getSemester().getAcademicYear().getId(),
                    p.getStartDate(), p.getEndDate());
        }
    }

    public record ElementView(UUID id, String code, String name,
                              UUID moduleId, String moduleCode, UUID programId,
                              PeriodView period) {
        public static ElementView from(Element e) {
            return new ElementView(e.getId(), e.getCode(), e.getName(),
                    e.getModule().getId(), e.getModule().getCode(),
                    e.getModule().getProgram().getId(),
                    PeriodView.from(e.getPeriod()));
        }
    }

    public record GroupView(UUID id, String name, Integer studyLevel,
                            UUID programId, UUID departmentId, UUID academicYearId) {
        public static GroupView from(StudentGroup g) {
            return new GroupView(g.getId(), g.getName(), g.getStudyLevel(),
                    g.getProgram().getId(), g.getProgram().getDepartment().getId(),
                    g.getAcademicYear().getId());
        }
    }
    

    public record AssignmentView(UUID id, UUID elementId, UUID teacherUserId, UUID groupId,
                                 UUID periodId, ActivityType activityType, BigDecimal targetHours) {
        public static AssignmentView from(TeachingAssignment a) {
            return new AssignmentView(a.getId(), a.getElement().getId(), a.getTeacher().getId(),
                    a.getStudentGroup().getId(), a.getPeriod().getId(),
                    a.getActivityType(), a.getTargetHours());
        }
    }
        public record DepartmentView(UUID id, String code, String name, UUID headUserId) {
        public static DepartmentView from(Department d) {
            return new DepartmentView(d.getId(), d.getCode(), d.getName(), d.getHeadUserId());
        }
    }

    public record ProgramView(UUID id, String code, String name, UUID departmentId, UUID headUserId) {
        public static ProgramView from(Program p) {
            return new ProgramView(p.getId(), p.getCode(), p.getName(),
                    p.getDepartment().getId(), p.getHeadUserId());
        }
    }

    public record AssignmentCheckView(boolean assigned, List<AssignmentView> assignments) {
    }
}