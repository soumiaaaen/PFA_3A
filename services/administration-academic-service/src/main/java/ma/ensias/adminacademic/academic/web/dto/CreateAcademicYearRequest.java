package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateAcademicYearRequest(
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{4}", message = "Format attendu : 2026-2027") String label,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}