package ma.ensias.adminacademic.identity.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.UserRepository;
import ma.ensias.adminacademic.identity.web.dto.LoginRequest;
import ma.ensias.adminacademic.identity.web.dto.TokenResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Identifiants invalides");
        }
        if (!user.isActive()) {
            throw new DisabledException("Compte désactivé");
        }
        return buildTokens(user);
    }

    public TokenResponse refresh(String refreshToken) {
        User user = refreshTokenService.consume(refreshToken);
        return buildTokens(user);
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private TokenResponse buildTokens(User user) {
        return new TokenResponse(
                jwtService.generateToken(user),
                refreshTokenService.issue(user),
                "Bearer",
                jwtService.expirationSeconds());
    }
}