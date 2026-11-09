package ma.ensias.adminacademic.academic.web.dto;

import ma.ensias.adminacademic.academic.entity.Period;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodResponse(UUID id, UUID semesterId, Integer number,
                             LocalDate startDate, LocalDate endDate) {

    public static PeriodResponse from(Period p) {
        return new PeriodResponse(p.getId(), p.getSemester().getId(), p.getNumber(),
                p.getStartDate(), p.getEndDate());
    }
}