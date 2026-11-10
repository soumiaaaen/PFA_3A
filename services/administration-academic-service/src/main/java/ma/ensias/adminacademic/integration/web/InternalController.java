package ma.ensias.adminacademic.integration.web;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.integration.service.InternalQueryService;
import ma.ensias.adminacademic.integration.web.InternalViews.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** API réservée aux autres microservices (en-tête X-Internal-Key obligatoire). */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final InternalQueryService service;

    @GetMapping("/users/{id}")
    public UserView user(@PathVariable UUID id) {
        return service.user(id);
    }

    @GetMapping("/periods/{id}")
    public PeriodView period(@PathVariable UUID id) {
        return service.period(id);
    }

    @GetMapping("/elements/{id}")
    public ElementView element(@PathVariable UUID id) {
        return service.element(id);
    }

    @GetMapping("/groups/{id}")
    public GroupView group(@PathVariable UUID id) {
        return service.group(id);
    }

    @GetMapping("/groups/{id}/students")
    public List<UUID> groupStudents(@PathVariable UUID id) {
        return service.groupStudentIds(id);
    }

    @GetMapping("/students/{userId}/groups")
    public List<GroupView> studentGroups(@PathVariable UUID userId) {
        return service.studentGroups(userId);
    }
        @GetMapping("/departments")
    public List<DepartmentView> departments(@RequestParam(required = false) UUID headUserId) {
        return service.departments(headUserId);
    }

    @GetMapping("/programs")
    public List<ProgramView> programs(@RequestParam(required = false) UUID headUserId,
                                      @RequestParam(required = false) UUID departmentId) {
        return service.programs(headUserId, departmentId);
    }

    @GetMapping("/assignments")
    public List<AssignmentView> assignments(@RequestParam UUID periodId,
                                            @RequestParam(required = false) UUID teacherUserId,
                                            @RequestParam(required = false) UUID groupId,
                                            @RequestParam(required = false) UUID elementId) {
        return service.assignments(periodId, teacherUserId, groupId, elementId);
    }

    @GetMapping("/assignments/check")
    public AssignmentCheckView check(@RequestParam UUID elementId,
                                     @RequestParam UUID teacherUserId,
                                     @RequestParam UUID groupId) {
        return service.checkAssignment(elementId, teacherUserId, groupId);
    }
}