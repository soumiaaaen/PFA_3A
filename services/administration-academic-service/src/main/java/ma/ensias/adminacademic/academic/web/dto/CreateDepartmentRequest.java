package ma.ensias.adminacademic.academic.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDepartmentRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 150) String name) {
}