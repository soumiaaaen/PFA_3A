package ma.ensias.adminacademic.identity.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.audit.service.AuditService;
import ma.ensias.adminacademic.common.exception.BadRequestException;
import ma.ensias.adminacademic.common.exception.ConflictException;
import ma.ensias.adminacademic.common.exception.NotFoundException;
import ma.ensias.adminacademic.identity.entity.Role;
import ma.ensias.adminacademic.identity.entity.RoleCode;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.RoleRepository;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.identity.web.dto.CreateUserRequest;
import ma.ensias.adminacademic.identity.web.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Un compte existe déjà avec cet email");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setFirstName(req.firstName().trim());
        user.setLastName(req.lastName().trim());
        user.setRoles(resolveRoles(req.roles()));
        User saved = userRepository.save(user);

        auditService.record("USER_CREATED", "USER", saved.getId(),
                "email=" + email + ", roles=" + req.roles());
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID id) {
        return UserResponse.from(getUser(id));
    }

    @Transactional
    public UserResponse updateRoles(UUID id, Set<RoleCode> codes) {
        User user = getUser(id);
        user.setRoles(resolveRoles(codes));
        auditService.record("USER_ROLES_CHANGED", "USER", id, "roles=" + codes);
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse setActive(UUID id, boolean active) {
        User user = getUser(id);
        user.setActive(active);
        if (!active) {
            refreshTokenService.revokeAll(id);
        }
        auditService.record(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", "USER", id, null);
        return UserResponse.from(user);
    }

    @Transactional
    public void changeOwnPassword(UUID userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadRequestException("Mot de passe actuel incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(userId);
        auditService.record("PASSWORD_CHANGED", "USER", userId, null);
    }

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        User user = getUser(userId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenService.revokeAll(userId);
        auditService.record("PASSWORD_RESET", "USER", userId, null);
    }

    private User getUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + id));
    }

    private Set<Role> resolveRoles(Set<RoleCode> codes) {
        return new HashSet<>(roleRepository.findByCodeIn(codes));
    }
}