package ma.ensias.adminacademic.assignment.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ma.ensias.adminacademic.assignment.entity.ActivityType;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull UUID elementId,
        @NotNull UUID teacherUserId,
        @NotNull UUID groupId,
        @NotNull ActivityType activityType,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal targetHours) {
}