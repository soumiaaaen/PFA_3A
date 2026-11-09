package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignHeadRequest(@NotNull UUID userId) {
}