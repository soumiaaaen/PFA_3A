package ma.ensias.adminacademic.identity.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull Boolean active) {
}