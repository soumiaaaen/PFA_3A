package ma.ensias.content.document.service;

import ma.ensias.content.common.exception.BadRequestException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Vérifie un fichier déposé : type RÉEL (signature du contenu, pas l'extension),
 * taille, et calcule son empreinte SHA-256. Formats acceptés (conception, décision 2) : PDF, DOCX, PPTX.
 */
@Component
public class FileInspector {

    public enum FileType {
        PDF("application/pdf", "pdf"),
        DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        PPTX("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx");

        public final String mimeType;
        public final String extension;

        FileType(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }

    /** Fichier vérifié, copié dans un fichier temporaire (à supprimer après usage). */
    public record InspectedFile(Path tempFile, long size, String sha256, FileType type, String originalFilename) {
    }

    public InspectedFile inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        Path temp = null;
        try {
            temp = Files.createTempFile("upload-", ".bin");
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new DigestInputStream(file.getInputStream(), sha)) {
                Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            }
            FileType type = detect(temp);
            return new InspectedFile(temp, Files.size(temp), HexFormat.of().formatHex(sha.digest()),
                    type, cleanFilename(file.getOriginalFilename(), type));
        } catch (IOException | NoSuchAlgorithmException e) {
            deleteQuietly(temp);
            throw new BadRequestException("Lecture du fichier impossible");
        } catch (RuntimeException e) {
            deleteQuietly(temp);
            throw e;
        }
    }

    public void deleteQuietly(Path temp) {
        if (temp != null) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // fichier temporaire : le système le nettoiera
            }
        }
    }

    private FileType detect(Path file) throws IOException {
        byte[] head = new byte[5];
        try (InputStream in = Files.newInputStream(file)) {
            int n = in.readNBytes(head, 0, head.length);
            if (n >= 5 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F' && head[4] == '-') {
                return FileType.PDF;
            }
            if (n >= 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4) {
                return detectOffice(file);
            }
        }
        throw new BadRequestException("Format refusé : seuls les fichiers PDF, DOCX et PPTX sont acceptés");
    }

    /** DOCX et PPTX sont des archives ZIP : on regarde leur contenu (au plus 2000 entrées). */
    private FileType detectOffice(Path file) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            int count = 0;
            while ((entry = zip.getNextEntry()) != null && count++ < 2000) {
                if (entry.getName().equals("word/document.xml")) {
                    return FileType.DOCX;
                }
                if (entry.getName().equals("ppt/presentation.xml")) {
                    return FileType.PPTX;
                }
            }
        }
        throw new BadRequestException("Format refusé : seuls les fichiers PDF, DOCX et PPTX sont acceptés");
    }

    /** Garde seulement le nom (sans chemin), sans caractères de contrôle, 255 caractères max, bonne extension. */
    private String cleanFilename(String original, FileType type) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").strip();
        if (name.isEmpty()) {
            name = "document";
        }
        String ext = "." + type.extension;
        if (!name.toLowerCase().endsWith(ext)) {
            name = name + ext;
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }
}
