package ma.ensias.adminacademic.catalog.web.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateModuleRequest(
        @NotNull UUID programId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 200) String name,
        @NotNull @Min(1) @Max(3) Integer studyLevel,
        String description,
        String objectives,
        @Positive BigDecimal coefficient) {
}