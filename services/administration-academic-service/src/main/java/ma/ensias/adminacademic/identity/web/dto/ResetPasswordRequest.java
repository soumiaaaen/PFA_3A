package ma.ensias.adminacademic.identity.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String newPassword) {
}