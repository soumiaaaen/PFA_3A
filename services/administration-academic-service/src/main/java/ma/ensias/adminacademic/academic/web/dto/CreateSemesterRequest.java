package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateSemesterRequest(
        @NotNull @Min(1) @Max(2) Integer number,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull LocalDate period1EndDate) {
}