package com.medical.messaging.controller;

import com.medical.messaging.dto.AttachmentDownload;
import com.medical.messaging.dto.MessageResponse;
import com.medical.messaging.dto.SendMessageRequest;
import com.medical.messaging.security.MessagingPrincipal;
import com.medical.messaging.service.MessageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

  private final MessageService messageService;

  public MessageController(MessageService messageService) {
    this.messageService = messageService;
  }

  @PostMapping
  public MessageResponse send(@RequestBody @Valid SendMessageRequest request) {
    return messageService.send(request, requirePrincipal());
  }

  @GetMapping("/patient/{id}")
  public List<MessageResponse> listForPatient(@PathVariable("id") Long patientId) {
    MessagingPrincipal p = requirePrincipal();
    if (p instanceof MessagingPrincipal.MessagingPatient mp && mp.patientId().equals(patientId)) {
      return messageService.listForPatient(patientId);
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  @GetMapping("/practitioner/{id}")
  public List<MessageResponse> listForPractitioner(@PathVariable("id") Long practitionerProfileId) {
    MessagingPrincipal p = requirePrincipal();
    if (p instanceof MessagingPrincipal.MessagingPractitioner pr
        && pr.practitionerProfileId() != null
        && pr.practitionerProfileId().equals(practitionerProfileId)) {
      return messageService.listForPractitioner(practitionerProfileId);
    }
    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }

  @PatchMapping("/{id}/read")
  public MessageResponse markRead(@PathVariable("id") Long messageId) {
    return messageService.markRead(messageId, requirePrincipal());
  }

  @GetMapping("/{messageId}/attachments/{attachmentId}")
  public ResponseEntity<Resource> downloadAttachment(
      @PathVariable("messageId") Long messageId,
      @PathVariable("attachmentId") Long attachmentId) {
    AttachmentDownload dl = messageService.readAttachment(messageId, attachmentId, requirePrincipal());
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    try {
      if (dl.fileType() != null && !dl.fileType().isBlank()) {
        mediaType = MediaType.parseMediaType(dl.fileType());
      }
    } catch (Exception ignored) {
    }
    ByteArrayResource body = new ByteArrayResource(dl.data());
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + dl.fileName().replace("\"", "") + "\"")
        .body(body);
  }

  private static MessagingPrincipal requirePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof MessagingPrincipal principal)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return principal;
  }
}
