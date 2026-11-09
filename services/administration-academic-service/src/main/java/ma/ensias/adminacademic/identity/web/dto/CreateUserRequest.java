package ma.ensias.adminacademic.identity.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import ma.ensias.adminacademic.identity.entity.RoleCode;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "8 caractères minimum") String password,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotEmpty Set<RoleCode> roles) {
}