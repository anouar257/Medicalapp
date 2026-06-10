package com.medical.messaging.service;

import com.medical.messaging.client.AgendaMessagingClient;
import com.medical.messaging.client.PatientMessagingAuthorizationClient;
import com.medical.messaging.client.PractitionerMessagingDisplayClient;
import com.medical.messaging.dto.AttachmentDownload;
import com.medical.messaging.dto.AttachmentPayload;
import com.medical.messaging.dto.AttachmentSummaryDto;
import com.medical.messaging.dto.MessageDirection;
import com.medical.messaging.dto.MessageResponse;
import com.medical.messaging.dto.SendMessageRequest;
import com.medical.messaging.entity.Attachment;
import com.medical.messaging.entity.Message;
import com.medical.messaging.repository.AttachmentRepository;
import com.medical.messaging.repository.MessageRepository;
import com.medical.messaging.security.MessagingPrincipal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

  private static final Logger log = LoggerFactory.getLogger(MessageService.class);

  private static final String MIME_JPEG = "image/jpeg";
  private static final String MIME_PNG = "image/png";
  private static final String MIME_WEBP = "image/webp";
  private static final String MIME_PDF = "application/pdf";

  private static final Set<String> ALLOWED_ATTACHMENT_MIMES =
      Set.of(MIME_JPEG, MIME_PNG, MIME_WEBP, MIME_PDF);

  private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_BY_MIME =
      Map.of(
          MIME_JPEG, Set.of("jpg", "jpeg"),
          MIME_PNG, Set.of("png"),
          MIME_WEBP, Set.of("webp"),
          MIME_PDF, Set.of("pdf"));

  private final MessageRepository messageRepository;
  private final AttachmentRepository attachmentRepository;
  private final AgendaMessagingClient agendaMessagingClient;
  private final PatientMessagingAuthorizationClient patientMessagingAuthorizationClient;
  private final PractitionerMessagingDisplayClient practitionerMessagingDisplayClient;
  private final PatientMessageSendRateLimiter patientMessageSendRateLimiter;
  private final long maxAttachmentBytesTotal;

  public MessageService(
      MessageRepository messageRepository,
      AttachmentRepository attachmentRepository,
      AgendaMessagingClient agendaMessagingClient,
      PatientMessagingAuthorizationClient patientMessagingAuthorizationClient,
      PractitionerMessagingDisplayClient practitionerMessagingDisplayClient,
      PatientMessageSendRateLimiter patientMessageSendRateLimiter,
      @Value("${app.messaging.max-attachment-bytes-total:5242880}") long maxAttachmentBytesTotal) {
    this.messageRepository = messageRepository;
    this.attachmentRepository = attachmentRepository;
    this.agendaMessagingClient = agendaMessagingClient;
    this.patientMessagingAuthorizationClient = patientMessagingAuthorizationClient;
    this.practitionerMessagingDisplayClient = practitionerMessagingDisplayClient;
    this.patientMessageSendRateLimiter = patientMessageSendRateLimiter;
    this.maxAttachmentBytesTotal = maxAttachmentBytesTotal;
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> listForPatient(Long patientUserId) {
    List<MessageResponse> list =
        messageRepository.findPatientMailbox(patientUserId).stream().map(this::toDto).toList();
    enrichDisplayNames(list);
    return list;
  }

  @Transactional(readOnly = true)
  public List<MessageResponse> listForPractitioner(Long practitionerProfileId) {
    List<MessageResponse> list =
        messageRepository.findPractitionerMailbox(practitionerProfileId).stream()
            .map(this::toDto)
            .toList();
    enrichDisplayNames(list);
    return list;
  }

  @Transactional
  public MessageResponse send(SendMessageRequest req, MessagingPrincipal principal) {
    validateSendShape(req);
    return switch (req.getDirection()) {
      case PATIENT_TO_PRACTITIONER -> sendPatientToPractitioner(req, principal);
      case PRACTITIONER_TO_PATIENT -> sendPractitionerToPatient(req, principal);
    };
  }

  @Transactional(readOnly = true)
  public AttachmentDownload readAttachment(
      long messageId, long attachmentId, MessagingPrincipal principal) {
    Attachment att =
        attachmentRepository
            .findByIdAndMessageId(attachmentId, messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
    Message m = att.getMessage();
    assertParticipantMayViewMessage(m, principal);
    return new AttachmentDownload(att.getFileName(), att.getFileType(), att.getData());
  }

  @Transactional
  public MessageResponse markRead(Long messageId, MessagingPrincipal principal) {
    Message m =
        messageRepository
            .findById(messageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message introuvable"));
    if ((principal instanceof MessagingPrincipal.MessagingPatient p
            && !Objects.equals(m.getReceiverPatientId(), p.patientId()))
        || (principal instanceof MessagingPrincipal.MessagingPractitioner pr
            && (pr.practitionerProfileId() == null
                || !Objects.equals(m.getReceiverPractitionerProfileId(), pr.practitionerProfileId())))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    m.setRead(true);
    return enrichSingle(toDto(m));
  }

  private void validateSendShape(SendMessageRequest req) {
    if (req.getDirection() == MessageDirection.PATIENT_TO_PRACTITIONER) {
      if (req.getReceiverPractitionerProfileId() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "receiverPractitionerProfileId requis pour PATIENT_TO_PRACTITIONER");
      }
    } else {
      if (req.getReceiverPatientId() == null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "receiverPatientId requis pour PRACTITIONER_TO_PATIENT");
      }
    }
  }

  private MessageResponse sendPatientToPractitioner(
      SendMessageRequest req, MessagingPrincipal principal) {
    if (!(principal instanceof MessagingPrincipal.MessagingPatient mp)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux patients");
    }
    Long ownerId = mp.patientId();
    assertCanRepresent(ownerId, req.getConcernedPersonId());
    assertAgendaRelationship(ownerId, req.getReceiverPractitionerProfileId(), true);
    patientMessageSendRateLimiter.acquireOrThrow(ownerId);
    validatePatientMessagingLimits(ownerId, req.getReceiverPractitionerProfileId());

    Message m = new Message();
    m.setSenderPatientId(ownerId);
    m.setReceiverPractitionerProfileId(req.getReceiverPractitionerProfileId());
    m.setConcernedPersonId(req.getConcernedPersonId());
    m.setContent(req.getContent());
    m.setSubject(req.getSubject());
    addAttachments(m, req.getAttachments());
    return enrichSingle(toDto(messageRepository.save(m)));
  }

  private MessageResponse sendPractitionerToPatient(
      SendMessageRequest req, MessagingPrincipal principal) {
    if (!(principal instanceof MessagingPrincipal.MessagingPractitioner pp)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Réservé aux praticiens");
    }
    if (pp.practitionerProfileId() == null) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Jeton sans profil praticien — impossible d'envoyer un message");
    }
    if (!Objects.equals(req.getConcernedPersonId(), req.getReceiverPatientId())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "concernedPersonId doit être identique au patient destinataire");
    }
    assertAgendaRelationship(req.getReceiverPatientId(), pp.practitionerProfileId(), false);

    Message m = new Message();
    m.setSenderPractitionerProfileId(pp.practitionerProfileId());
    m.setReceiverPatientId(req.getReceiverPatientId());
    m.setConcernedPersonId(req.getConcernedPersonId());
    m.setContent(req.getContent());
    m.setSubject(req.getSubject());
    addAttachments(m, req.getAttachments());
    return enrichSingle(toDto(messageRepository.save(m)));
  }

  private void assertCanRepresent(Long ownerPatientId, Long concernedPersonId) {
    Boolean allowed =
        patientMessagingAuthorizationClient
            .canRepresent(ownerPatientId, concernedPersonId)
            .get("allowed");
    if (!Boolean.TRUE.equals(allowed)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Vous ne pouvez pas envoyer un message pour cette personne concernée");
    }
  }

  private void assertAgendaRelationship(Long patientId, Long externalPractitionerProfileId, boolean completedOnly) {
    Boolean exists =
        agendaMessagingClient
            .hasRelationship(patientId, externalPractitionerProfileId, completedOnly)
            .get("exists");
    if (!Boolean.TRUE.equals(exists)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          completedOnly
              ? "Aucun rendez-vous terminé entre ce patient et ce praticien — envoi de message refusé"
              : "Aucun rendez-vous actif entre ce patient et ce praticien — envoi de message refusé");
    }
  }

  private void validatePatientMessagingLimits(Long patientId, Long practitionerId) {
    List<Message> latest = messageRepository.findConversation(
        patientId, practitionerId, org.springframework.data.domain.Limit.of(20));

    // 1. Check consecutive limit (max 3 consecutive messages without doctor reply)
    int consecutive = 0;
    for (Message m : latest) {
      if (m.getSenderPatientId() != null) {
        consecutive++;
      } else {
        break;
      }
    }
    if (consecutive >= 3) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Vous avez envoyé 3 messages consécutifs sans réponse de ce praticien. Veuillez attendre sa réponse avant d'envoyer un nouveau message.");
    }

    // 2. Check daily limit (max 10 messages in the last 24 hours)
    java.time.Instant oneDayAgo = java.time.Instant.now().minus(24, java.time.temporal.ChronoUnit.HOURS);
    long dailyCount = latest.stream()
        .filter(m -> m.getSenderPatientId() != null && m.getSentAt().isAfter(oneDayAgo))
        .count();
    if (dailyCount >= 10) {
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Limite quotidienne atteinte : vous ne pouvez pas envoyer plus de 10 messages par 24 heures à ce praticien.");
    }
  }

  private void addAttachments(Message m, List<AttachmentPayload> payloads) {
    if (payloads == null || payloads.isEmpty()) {
      return;
    }
    long total = 0;
    for (AttachmentPayload pl : payloads) {
      String normalizedMime = validateAttachmentMetadata(pl);
      byte[] bytes;
      try {
        bytes = Base64.getDecoder().decode(pl.getBase64Data().trim());
      } catch (IllegalArgumentException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pièce jointe Base64 invalide");
      }
      assertDecodedBytesMatchDeclaredMime(bytes, normalizedMime);
      total += bytes.length;
      if (total > maxAttachmentBytesTotal) {
        throw new ResponseStatusException(
            HttpStatus.PAYLOAD_TOO_LARGE, "Volume total des pièces jointes dépassé");
      }
      Attachment a = new Attachment();
      a.setFileName(pl.getFileName().trim());
      a.setFileType(normalizedMime);
      a.setData(bytes);
      m.addAttachment(a);
    }
  }

  /**
   * Vérifie le nom, le type MIME déclaré et l'extension. Types autorisés : image/jpeg, image/png,
   * image/webp, application/pdf.
   */
  private static String validateAttachmentMetadata(AttachmentPayload pl) {
    if (pl.getFileName() == null || pl.getFileName().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Format de fichier non autorisé : nom de fichier obligatoire pour chaque pièce jointe.");
    }
    String mime = normalizeMimeType(pl.getFileType());
    if (!ALLOWED_ATTACHMENT_MIMES.contains(mime)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Format de fichier non autorisé : types acceptés image/jpeg, image/png, image/webp, application/pdf.");
    }
    String ext = extractExtension(pl.getFileName());
    if (ext.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Format de fichier non autorisé : le nom de fichier doit comporter une extension (.jpg, .png, .webp, .pdf).");
    }
    Set<String> allowedExt = ALLOWED_EXTENSIONS_BY_MIME.get(mime);
    if (allowedExt == null || !allowedExt.contains(ext)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Format de fichier non autorisé : l'extension ne correspond pas au type MIME déclaré.");
    }
    return mime;
  }

  private static String normalizeMimeType(String fileType) {
    if (fileType == null || fileType.isBlank()) {
      return "";
    }
    String t = fileType.trim().toLowerCase(Locale.ROOT);
    int semi = t.indexOf(';');
    if (semi >= 0) {
      t = t.substring(0, semi).trim();
    }
    return t;
  }

  private static String extractExtension(String fileName) {
    String base = fileName.trim();
    int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
    if (slash >= 0 && slash < base.length() - 1) {
      base = base.substring(slash + 1);
    }
    int dot = base.lastIndexOf('.');
    if (dot < 0 || dot == base.length() - 1) {
      return "";
    }
    return base.substring(dot + 1).trim().toLowerCase(Locale.ROOT);
  }

  private static void assertDecodedBytesMatchDeclaredMime(byte[] data, String mime) {
    switch (mime) {
      case MIME_PDF:
        if (data.length < 5
            || !new String(Arrays.copyOfRange(data, 0, 5), StandardCharsets.US_ASCII).startsWith("%PDF")) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Format de fichier non autorisé : le contenu ne correspond pas à un document PDF.");
        }
        break;
      case MIME_JPEG:
        if (data.length < 3
            || (data[0] & 0xFF) != 0xFF
            || (data[1] & 0xFF) != 0xD8
            || (data[2] & 0xFF) != 0xFF) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Format de fichier non autorisé : le contenu ne correspond pas à une image JPEG.");
        }
        break;
      case MIME_PNG:
        if (data.length < 8
            || (data[0] & 0xFF) != 0x89
            || data[1] != 0x50
            || data[2] != 0x4E
            || data[3] != 0x47
            || data[4] != 0x0D
            || data[5] != 0x0A
            || data[6] != 0x1A
            || data[7] != 0x0A) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Format de fichier non autorisé : le contenu ne correspond pas à une image PNG.");
        }
        break;
      case MIME_WEBP:
        if (data.length < 12
            || data[0] != 'R'
            || data[1] != 'I'
            || data[2] != 'F'
            || data[3] != 'F'
            || data[8] != 'W'
            || data[9] != 'E'
            || data[10] != 'B'
            || data[11] != 'P') {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Format de fichier non autorisé : le contenu ne correspond pas à une image WebP.");
        }
        break;
      default:
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Format de fichier non autorisé : types acceptés image/jpeg, image/png, image/webp, application/pdf.");
    }
  }

  private MessageResponse toDto(Message m) {
    MessageResponse r = new MessageResponse();
    r.setId(m.getId());
    r.setSenderPatientId(m.getSenderPatientId());
    r.setSenderPractitionerProfileId(m.getSenderPractitionerProfileId());
    r.setReceiverPatientId(m.getReceiverPatientId());
    r.setReceiverPractitionerProfileId(m.getReceiverPractitionerProfileId());
    r.setConcernedPersonId(m.getConcernedPersonId());
    r.setContent(m.getContent());
    r.setSubject(m.getSubject());
    r.setSentAt(m.getSentAt());
    r.setRead(m.isRead());
    List<AttachmentSummaryDto> sums = new ArrayList<>();
    for (Attachment att : m.getAttachments()) {
      AttachmentSummaryDto s = new AttachmentSummaryDto();
      s.setId(att.getId());
      s.setFileName(att.getFileName());
      s.setFileType(att.getFileType());
      sums.add(s);
    }
    r.setAttachments(sums);
    return r;
  }

  private MessageResponse enrichSingle(MessageResponse r) {
    enrichDisplayNames(List.of(r));
    return r;
  }

  private void enrichDisplayNames(List<MessageResponse> responses) {
    if (responses == null || responses.isEmpty()) {
      return;
    }
    Set<Long> patientSideIds = new HashSet<>();
    Set<Long> practitionerIds = new HashSet<>();
    for (MessageResponse r : responses) {
      addIfNonNull(patientSideIds, r.getSenderPatientId());
      addIfNonNull(patientSideIds, r.getReceiverPatientId());
      addIfNonNull(patientSideIds, r.getConcernedPersonId());
      addIfNonNull(practitionerIds, r.getSenderPractitionerProfileId());
      addIfNonNull(practitionerIds, r.getReceiverPractitionerProfileId());
    }
    Map<Long, String> patientNames = fetchPatientDisplayNames(new ArrayList<>(patientSideIds));
    Map<Long, String> practitionerNames = fetchPractitionerDisplayNames(new ArrayList<>(practitionerIds));
    for (MessageResponse r : responses) {
      r.setSenderName(
          resolvePartyName(r.getSenderPatientId(), r.getSenderPractitionerProfileId(), patientNames, practitionerNames));
      r.setReceiverName(
          resolvePartyName(
              r.getReceiverPatientId(), r.getReceiverPractitionerProfileId(), patientNames, practitionerNames));
      r.setConcernedPersonName(lookupName(patientNames, r.getConcernedPersonId()));
    }
  }

  private static void addIfNonNull(Set<Long> set, Long id) {
    if (id != null) {
      set.add(id);
    }
  }

  private static String lookupName(Map<Long, String> map, Long id) {
    if (id == null) {
      return null;
    }
    return map.get(id);
  }

  private static String resolvePartyName(
      Long patientId,
      Long practitionerProfileId,
      Map<Long, String> patientNames,
      Map<Long, String> practitionerNames) {
    if (patientId != null) {
      String n = patientNames.get(patientId);
      if (n != null) {
        return n;
      }
    }
    if (practitionerProfileId != null) {
      return practitionerNames.get(practitionerProfileId);
    }
    return null;
  }

  private Map<Long, String> fetchPatientDisplayNames(List<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    try {
      Map<Long, String> map = patientMessagingAuthorizationClient.personDisplayNames(ids);
      return map != null ? map : Map.of();
    } catch (RuntimeException e) {
      log.warn("patient-service person-display-names indisponible: {}", e.toString());
      return Map.of();
    }
  }

  private Map<Long, String> fetchPractitionerDisplayNames(List<Long> ids) {
    if (ids.isEmpty()) {
      return Map.of();
    }
    try {
      Map<Long, String> map = practitionerMessagingDisplayClient.practitionerDisplayNames(ids);
      return map != null ? map : Map.of();
    } catch (RuntimeException e) {
      log.warn("practitioner-service practitioner-display-names indisponible: {}", e.toString());
      return Map.of();
    }
  }

  private void assertParticipantMayViewMessage(Message m, MessagingPrincipal principal) {
    if (principal instanceof MessagingPrincipal.MessagingPatient p) {
      if (!patientMayViewMessage(m, p.patientId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    } else if (principal instanceof MessagingPrincipal.MessagingPractitioner pr) {
      if (!practitionerMayViewMessage(m, pr.practitionerProfileId())) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
      }
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  private static boolean patientMayViewMessage(Message m, Long patientId) {
    return Objects.equals(m.getSenderPatientId(), patientId)
        || Objects.equals(m.getReceiverPatientId(), patientId)
        || Objects.equals(m.getConcernedPersonId(), patientId);
  }

  private static boolean practitionerMayViewMessage(Message m, Long practitionerProfileId) {
    if (practitionerProfileId == null) {
      return false;
    }
    return Objects.equals(m.getSenderPractitionerProfileId(), practitionerProfileId)
        || Objects.equals(m.getReceiverPractitionerProfileId(), practitionerProfileId);
  }
}
