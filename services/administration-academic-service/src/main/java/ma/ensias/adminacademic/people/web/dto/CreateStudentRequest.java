package ma.ensias.adminacademic.people.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateStudentRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 30) String studentNumber) {
}