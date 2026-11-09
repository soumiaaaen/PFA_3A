package ma.ensias.adminacademic.identity.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.identity.service.UserService;
import ma.ensias.adminacademic.identity.web.dto.CreateUserRequest;
import ma.ensias.adminacademic.identity.web.dto.UpdateRolesRequest;
import ma.ensias.adminacademic.identity.web.dto.UpdateStatusRequest;
import ma.ensias.adminacademic.identity.web.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ma.ensias.adminacademic.identity.web.dto.ResetPasswordRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable UUID id,
                                    @Valid @RequestBody UpdateRolesRequest request) {
        return userService.updateRoles(id, request.roles());
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable UUID id,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return userService.setActive(id, request.active());
    }
        @PostMapping("/{id}/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }
}