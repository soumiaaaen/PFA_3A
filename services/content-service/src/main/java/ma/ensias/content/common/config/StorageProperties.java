package ma.ensias.content.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Connexion à MinIO (API compatible S3). */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String endpoint, String accessKey, String secretKey, String bucket, String region) {
}
