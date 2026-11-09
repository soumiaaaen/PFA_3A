package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.AcademicYear;

import java.time.LocalDate;
import java.util.UUID;

public record AcademicYearResponse(UUID id, String label, LocalDate startDate, LocalDate endDate) {

    public static AcademicYearResponse from(AcademicYear y) {
        return new AcademicYearResponse(y.getId(), y.getLabel(), y.getStartDate(), y.getEndDate());
    }
}