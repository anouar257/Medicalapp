package com.medical.agenda.service;

import com.medical.agenda.config.AgendaProperties;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stockage des fichiers portrait sur disque. Les URLs publiques sont de la forme
 * {@code /api/doctor-files/{nomFichier}}.
 */
@Service
public class DoctorPhotoFileService {

  public static final String PUBLIC_PREFIX = "/api/doctor-files/";

  private static final Pattern SAFE_FILENAME =
      Pattern.compile(
          "^d[0-9]+-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|jpeg|png|webp|gif)$",
          Pattern.CASE_INSENSITIVE);

  private static final Set<String> ALLOWED_CT =
      Set.of(
          MediaType.IMAGE_JPEG_VALUE,
          "image/jpg",
          "image/pjpeg",
          MediaType.IMAGE_PNG_VALUE,
          "image/webp",
          "image/gif");

  private final AgendaProperties properties;
  private Path uploadDir;

  public DoctorPhotoFileService(AgendaProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void init() throws IOException {
    uploadDir = Paths.get(properties.getDirectory()).toAbsolutePath().normalize();
    Files.createDirectories(uploadDir);
  }

  /**
   * Enregistre le fichier et retourne l’URL relative à enregistrer en base (ex.
   * {@code /api/doctor-files/d3-….jpeg}).
   */
  public String store(Long doctorId, MultipartFile file) {
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
        "d" + doctorId + "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT) + "." + ext;
    Path target = uploadDir.resolve(filename).normalize();
    if (!target.startsWith(uploadDir)) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Chemin refusé.");
    }
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Impossible d’enregistrer le fichier.", e);
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

  /** Supprime un fichier précédemment servi sous {@link #PUBLIC_PREFIX} si présent sur disque. */
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

  public Resource loadAsResource(String filename) {
    if (filename == null || filename.isBlank() || !SAFE_FILENAME.matcher(filename).matches()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier inconnu.");
    }
    Path target = uploadDir.resolve(filename).normalize();
    if (!target.startsWith(uploadDir) || !Files.isReadable(target)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier inconnu.");
    }
    try {
      return new UrlResource(target.toUri());
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier inconnu.", e);
    }
  }

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
