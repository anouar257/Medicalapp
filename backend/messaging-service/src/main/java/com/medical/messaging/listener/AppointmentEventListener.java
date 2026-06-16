package com.medical.messaging.listener;

import com.medical.agenda.dto.AppointmentEvent;
import com.medical.messaging.entity.Message;
import com.medical.messaging.entity.Subject;
import com.medical.messaging.repository.MessageRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventListener {

  private static final Logger log = LoggerFactory.getLogger(AppointmentEventListener.class);
  private final MessageRepository messageRepository;

  public AppointmentEventListener(MessageRepository messageRepository) {
    this.messageRepository = messageRepository;
  }

  @KafkaListener(topics = "appointment-events", groupId = "messaging-group")
  public void handleAppointmentEvent(AppointmentEvent event) {
    log.info("Received appointment event: {}", event);

    if (event.patientId() == null || event.practitionerId() == null) {
      log.warn("Missing patientId or practitionerId in event, skipping notification message creation.");
      return;
    }

    try {
      String content = buildNotificationContent(event);
      Message message = new Message();
      message.setSenderPractitionerProfileId(event.practitionerId());
      message.setReceiverPatientId(event.patientId());
      message.setConcernedPersonId(event.patientId());
      message.setContent(content);
      message.setSubject(Subject.AUTRE);
      message.setSentAt(Instant.now());
      message.setRead(false);

      messageRepository.save(message);
      log.info("Created system notification message for appointmentId: {}", event.appointmentId());
    } catch (Exception e) {
      log.error("Failed to process appointment event and save message", e);
    }
  }

  private String buildNotificationContent(AppointmentEvent event) {
    String dateStr = event.dateTime() != null ? event.dateTime() : "inconnue";
    if (dateStr.contains("T")) {
      dateStr = dateStr.replace("T", " ").substring(0, Math.min(dateStr.length(), 16));
    }
    
    String status = event.status() != null ? event.status() : "UNKNOWN";
    return switch (status) {
      case "CONFIRMED" -> "Votre rendez-vous prévu le " + dateStr + " a été confirmé.";
      case "PENDING" -> "Votre demande de rendez-vous pour le " + dateStr + " est en attente de confirmation.";
      case "CANCELLED" -> "Votre rendez-vous prévu le " + dateStr + " a été annulé.";
      case "COMPLETED" -> "Votre rendez-vous du " + dateStr + " est maintenant terminé. Merci pour votre visite.";
      case "NO_SHOW" -> "Vous ne vous êtes pas présenté à votre rendez-vous du " + dateStr + ".";
      default -> "Le statut de votre rendez-vous prévu le " + dateStr + " a changé : " + status + ".";
    };
  }
}
