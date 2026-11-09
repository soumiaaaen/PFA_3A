package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateProgramRequest(
        @NotNull UUID departmentId,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 150) String name) {
}