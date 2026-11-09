package ma.ensias.adminacademic.academic.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.academic.service.AcademicCalendarService;
import ma.ensias.adminacademic.academic.web.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AcademicCalendarController {

    private final AcademicCalendarService service;

    @PostMapping("/academic-years")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public AcademicYearResponse createYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        return service.createYear(request);
    }

    @GetMapping("/academic-years")
    public List<AcademicYearResponse> listYears() {
        return service.listYears();
    }

    @GetMapping("/academic-years/{id}")
    public AcademicYearResponse getYear(@PathVariable UUID id) {
        return service.getYear(id);
    }

    @PostMapping("/academic-years/{yearId}/semesters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SemesterResponse createSemester(@PathVariable UUID yearId,
                                           @Valid @RequestBody CreateSemesterRequest request) {
        return service.createSemester(yearId, request);
    }

    @GetMapping("/academic-years/{yearId}/semesters")
    public List<SemesterResponse> listSemesters(@PathVariable UUID yearId) {
        return service.listSemesters(yearId);
    }

    @GetMapping("/periods/{id}")
    public PeriodResponse getPeriod(@PathVariable UUID id) {
        return service.getPeriod(id);
    }
}