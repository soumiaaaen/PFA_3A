package ma.ensias.adminacademic.academic.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.service.OrganizationService;
import ma.ensias.adminacademic.academic.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService service;

    // Départements
    @PostMapping("/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        return service.createDepartment(request);
    }

    @GetMapping("/departments")
    public List<DepartmentResponse> listDepartments() {
        return service.listDepartments();
    }

    @PutMapping("/departments/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public DepartmentResponse assignDepartmentHead(@PathVariable UUID id,
                                                   @Valid @RequestBody AssignHeadRequest request) {
        return service.assignDepartmentHead(id, request.userId());
    }

    // Filières
    @PostMapping("/programs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse createProgram(@Valid @RequestBody CreateProgramRequest request) {
        return service.createProgram(request);
    }

    @GetMapping("/programs")
    public List<ProgramResponse> listPrograms(@RequestParam(required = false) UUID departmentId) {
        return service.listPrograms(departmentId);
    }

    @PutMapping("/programs/{id}/head")
    @PreAuthorize("hasRole('ADMIN')")
    public ProgramResponse assignProgramHead(@PathVariable UUID id,
                                             @Valid @RequestBody AssignHeadRequest request) {
        return service.assignProgramHead(id, request.userId());
    }

    // Groupes
    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse createGroup(@Valid @RequestBody CreateGroupRequest request) {
        return service.createGroup(request);
    }

    @GetMapping("/groups")
    public List<GroupResponse> listGroups(@RequestParam UUID programId,
                                          @RequestParam UUID academicYearId) {
        return service.listGroups(programId, academicYearId);
    }

    @GetMapping("/groups/{id}")
    public GroupResponse getGroup(@PathVariable UUID id) {
        return service.getGroup(id);
    }
}