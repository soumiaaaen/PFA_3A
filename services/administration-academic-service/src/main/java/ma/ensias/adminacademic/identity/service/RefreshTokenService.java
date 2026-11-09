package ma.ensias.adminacademic.identity.service;

import lombok.RequiredArgsConstructor;
import ma.ensias.adminacademic.common.config.JwtProperties;
import ma.ensias.adminacademic.identity.entity.RefreshToken;
import ma.ensias.adminacademic.identity.entity.User;
import ma.ensias.adminacademic.identity.repository.RefreshTokenRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    /** Crée un refresh token et renvoie sa valeur en clair (elle ne sera plus jamais lisible). */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(props.refreshExpirationDays(), ChronoUnit.DAYS));
        repository.save(token);
        return raw;
    }

    /** Vérifie un refresh token, le révoque (usage unique) et renvoie son propriétaire. */
    @Transactional(noRollbackFor = AuthenticationException.class)
    public User consume(String raw) {
        RefreshToken token = repository.findByTokenHash(hash(raw))
                .orElseThrow(() -> new BadCredentialsException("Refresh token invalide"));

        if (token.getRevokedAt() != null) {
            // Un token déjà utilisé est rejoué : vol probable, on ferme toutes les sessions
            repository.revokeAllForUser(token.getUser().getId(), Instant.now());
            throw new BadCredentialsException("Refresh token déjà utilisé : toutes les sessions ont été fermées");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expiré");
        }
        if (!token.getUser().isActive()) {
            throw new DisabledException("Compte désactivé");
        }
        token.setRevokedAt(Instant.now());
        return token.getUser();
    }

    @Transactional
    public void revoke(String raw) {
        repository.findByTokenHash(hash(raw))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}