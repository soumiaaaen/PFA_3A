package ma.ensias.content.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ma.ensias.content.common.config.StorageProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

/**
 * Accès aux fichiers dans MinIO. Le bucket est privé : les fichiers ne sortent
 * jamais directement de MinIO, ils passent toujours par le content-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3;
    private final StorageProperties props;

    /** Crée le bucket au démarrage s'il n'existe pas encore. */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureBucket() {
        try {
            s3.headBucket(b -> b.bucket(props.bucket()));
            log.info("Bucket MinIO '{}' disponible", props.bucket());
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                s3.createBucket(b -> b.bucket(props.bucket()));
                log.info("Bucket MinIO '{}' créé", props.bucket());
            } else {
                log.error("MinIO inaccessible ({}) : {}", props.endpoint(), e.getMessage());
            }
        } catch (RuntimeException e) {
            log.error("MinIO inaccessible ({}) : {}", props.endpoint(), e.getMessage());
        }
    }

    public void put(String objectKey, InputStream content, long size, String contentType) {
        s3.putObject(b -> b.bucket(props.bucket()).key(objectKey).contentType(contentType),
                RequestBody.fromInputStream(content, size));
    }

    public ResponseInputStream<GetObjectResponse> get(String objectKey) {
        return s3.getObject(b -> b.bucket(props.bucket()).key(objectKey));
    }

    public void delete(String objectKey) {
        s3.deleteObject(b -> b.bucket(props.bucket()).key(objectKey));
    }

    public boolean isAvailable() {
        try {
            s3.headBucket(b -> b.bucket(props.bucket()));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
