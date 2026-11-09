package ma.ensias.adminacademic.people.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EnrollStudentRequest(@NotNull UUID studentUserId) {
}