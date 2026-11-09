package ma.ensias.adminacademic.people.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.people.service.StudentService;
import ma.ensias.adminacademic.people.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @PostMapping("/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StudentResponse createStudent(@Valid @RequestBody CreateStudentRequest request) {
        return service.createStudent(request);
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public List<StudentResponse> listStudents() {
        return service.listStudents();
    }

    @GetMapping("/students/me/groups")
    @PreAuthorize("hasRole('ETUDIANT')")
    public List<EnrollmentResponse> myGroups() {
        return service.myGroups();
    }

    @PostMapping("/groups/{groupId}/students")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public EnrollmentResponse enroll(@PathVariable UUID groupId,
                                     @Valid @RequestBody EnrollStudentRequest request) {
        return service.enroll(groupId, request);
    }

    @GetMapping("/groups/{groupId}/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE', 'CHEF_DEPARTEMENT', 'ENSEIGNANT')")
    public List<EnrollmentResponse> listGroupStudents(@PathVariable UUID groupId) {
        return service.listGroupStudents(groupId);
    }

    @DeleteMapping("/groups/{groupId}/students/{studentUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'CHEF_FILIERE')")
    public void unenroll(@PathVariable UUID groupId, @PathVariable UUID studentUserId) {
        service.unenroll(groupId, studentUserId);
    }
}