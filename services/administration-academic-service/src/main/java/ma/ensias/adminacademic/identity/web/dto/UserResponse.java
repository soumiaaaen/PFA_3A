package ma.ensias.adminacademic.identity.web.dto;

import ma.ensias.adminacademic.identity.entity.User;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active,
        Set<String> roles) {

    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFirstName(),
                u.getLastName(),
                u.isActive(),
                u.getRoles().stream().map(r -> r.getCode().name()).collect(Collectors.toSet()));
    }
}