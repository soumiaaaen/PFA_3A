package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.Semester;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SemesterResponse(UUID id, UUID academicYearId, String label, Integer number,
                               LocalDate startDate, LocalDate endDate, List<PeriodResponse> periods) {

    public static SemesterResponse from(Semester s) {
        return new SemesterResponse(
                s.getId(),
                s.getAcademicYear().getId(),
                "S" + s.getNumber() + " " + s.getAcademicYear().getLabel(),
                s.getNumber(),
                s.getStartDate(),
                s.getEndDate(),
                s.getPeriods().stream().map(PeriodResponse::from).toList());
    }
}