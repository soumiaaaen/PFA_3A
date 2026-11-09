package ma.ensias.adminacademic.identity.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.identity.service.AuthService;
import ma.ensias.adminacademic.identity.service.UserService;
import ma.ensias.adminacademic.identity.web.dto.LoginRequest;
import ma.ensias.adminacademic.identity.web.dto.RefreshRequest;
import ma.ensias.adminacademic.identity.web.dto.TokenResponse;
import ma.ensias.adminacademic.identity.web.dto.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ma.ensias.adminacademic.identity.web.dto.ChangePasswordRequest;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.findById(UUID.fromString(jwt.getSubject()));
    }
        @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }
        @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changeOwnPassword(UUID.fromString(jwt.getSubject()),
                request.currentPassword(), request.newPassword());
    }
}