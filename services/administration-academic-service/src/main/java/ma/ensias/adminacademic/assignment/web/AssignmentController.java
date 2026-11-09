package ma.ensias.adminacademic.assignment.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.assignment.service.AssignmentService;
import ma.ensias.adminacademic.assignment.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public AssignmentResponse create(@Valid @RequestBody CreateAssignmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public AssignmentResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateAssignmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public void deactivate(@PathVariable UUID id) {
        service.deactivate(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT', 'ENSEIGNANT')")
    public List<AssignmentResponse> list(@RequestParam UUID periodId,
                                         @RequestParam(required = false) UUID teacherUserId,
                                         @RequestParam(required = false) UUID groupId,
                                         @RequestParam(required = false) UUID elementId) {
        return service.list(periodId, teacherUserId, groupId, elementId);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT')")
    public List<HistoryResponse> history(@PathVariable UUID id) {
        return service.history(id);
    }
}