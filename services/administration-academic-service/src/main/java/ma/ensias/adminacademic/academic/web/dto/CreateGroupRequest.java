package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateGroupRequest(
        @NotNull UUID programId,
        @NotNull UUID academicYearId,
        @NotNull @Min(1) @Max(3) Integer studyLevel,
        @NotBlank @Size(max = 50) String name) {
}