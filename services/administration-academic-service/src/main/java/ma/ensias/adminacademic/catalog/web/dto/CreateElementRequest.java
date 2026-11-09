package ma.ensias.adminacademic.catalog.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateElementRequest(
        @NotNull UUID periodId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String name,
        String description,
        String objectives,
        @Positive BigDecimal coefficient) {
}