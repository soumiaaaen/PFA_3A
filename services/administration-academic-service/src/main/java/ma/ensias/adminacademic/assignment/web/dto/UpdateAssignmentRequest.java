package ma.ensias.adminacademic.assignment.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAssignmentRequest(
        @NotNull UUID teacherUserId,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal targetHours) {
}