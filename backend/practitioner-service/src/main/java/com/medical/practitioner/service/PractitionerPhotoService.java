package com.medical.practitioner.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PractitionerPhotoService {

    public static final String PUBLIC_PREFIX = "/api/pro/public/practitioners/photos/";

    private static final String ERROR_UNKNOWN_FILE = "Fichier inconnu.";

    private static final Pattern SAFE_FILENAME =
        Pattern.compile(
            "^p\\d+-[\\da-f]{8}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{4}-[\\da-f]{12}\\.(jpg|jpeg|png|webp|gif)$",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> ALLOWED_CT =
        Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            "image/jpg",
            "image/pjpeg",
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/gif");

    @Value("${practitioner.photos.directory}")
    private String directory;

    private Path uploadDir;

    @PostConstruct
    void init() throws IOException {
        uploadDir = Paths.get(directory).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    /**
     * Enregistre la photo et retourne l'URL publique correspondante.
     */
    public String store(Long practitionerId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier vide.");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CT.contains(ct.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formats acceptés : JPEG, PNG, WebP, GIF.");
        }
        String ext = extensionForContentType(ct);
        String filename =
            "p" + practitionerId + "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + "." + ext;
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chemin refusé.");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d'enregistrer le fichier.", e);
        }
        return PUBLIC_PREFIX + filename;
    }

    private static String extensionForContentType(String ct) {
        String c = ct.toLowerCase(Locale.ROOT);
        if (c.contains("jpeg") || c.contains("jpg")) {
            return "jpg";
        }
        if (c.contains("png")) {
            return "png";
        }
        if (c.contains("webp")) {
            return "webp";
        }
        if (c.contains("gif")) {
            return "gif";
        }
        return "jpg";
    }

    /**
     * Supprime une photo de profil du disque si elle existe.
     */
    public void deleteManagedIfPresent(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank() || !photoUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String filename = photoUrl.substring(PUBLIC_PREFIX.length());
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            return;
        }
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // ignore
        }
    }

    /**
     * Charge une photo en tant que ressource Spring.
     */
    public Resource loadAsResource(String filename) {
        if (filename == null || filename.isBlank() || !SAFE_FILENAME.matcher(filename).matches()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_UNKNOWN_FILE);
        }
        Path target = uploadDir.resolve(filename).normalize();
        if (!target.startsWith(uploadDir) || !Files.isReadable(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_UNKNOWN_FILE);
        }
        try {
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ERROR_UNKNOWN_FILE, e);
        }
    }

    /**
     * Détermine le MediaType approprié pour le fichier.
     */
    public MediaType probeMediaType(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }
}
