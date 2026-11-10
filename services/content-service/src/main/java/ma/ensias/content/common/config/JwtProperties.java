package ma.ensias.content.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Le content-service ne crée pas de tokens : il vérifie ceux signés par l'administration-academic-service. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer) {
}
