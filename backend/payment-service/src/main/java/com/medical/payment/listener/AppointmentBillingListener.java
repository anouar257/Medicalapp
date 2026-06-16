package com.medical.payment.listener;

import com.medical.agenda.dto.AppointmentEvent;
import com.medical.payment.entity.Payment;
import com.medical.payment.entity.PaymentStatus;
import com.medical.payment.repository.PaymentRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentBillingListener {

  private static final Logger log = LoggerFactory.getLogger(AppointmentBillingListener.class);
  private final PaymentRepository paymentRepository;

  public AppointmentBillingListener(PaymentRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  @KafkaListener(topics = "appointment-events", groupId = "payment-group")
  public void handleAppointmentEvent(AppointmentEvent event) {
    log.info("Received appointment event for billing: {}", event);

    if (event.appointmentId() == null) {
      log.warn("Missing appointmentId in event, skipping billing initialization.");
      return;
    }

    try {
      Optional<Payment> existingPaymentOpt = paymentRepository.findByAppointmentId(event.appointmentId());

      String status = event.status() != null ? event.status() : "UNKNOWN";

      if (existingPaymentOpt.isPresent()) {
        Payment existingPayment = existingPaymentOpt.get();
        if ("CANCELLED".equals(status)) {
          existingPayment.setStatus(PaymentStatus.FAILED);
          existingPayment.setDescription(existingPayment.getDescription() + " (Rendez-vous annulé)");
          paymentRepository.save(existingPayment);
          log.info("Updated payment status to FAILED for cancelled appointmentId: {}", event.appointmentId());
        } else if ("COMPLETED".equals(status)) {
          existingPayment.setStatus(PaymentStatus.COMPLETED);
          existingPayment.setDescription(existingPayment.getDescription() + " (Rendez-vous terminé)");
          paymentRepository.save(existingPayment);
          log.info("Updated payment status to COMPLETED for completed appointmentId: {}", event.appointmentId());
        }
      } else {
        // Only initialize billing for active/confirmed or pending/completed appointments
        if ("CONFIRMED".equals(status) || "PENDING".equals(status) || "COMPLETED".equals(status)) {
          Payment payment = new Payment();
          payment.setAppointmentId(event.appointmentId());
          payment.setPatientId(event.patientId());
          payment.setPatientName(event.patientId() != null ? "Patient #" + event.patientId() : "Patient Inconnu");
          payment.setOrganizationId(event.organizationId() != null ? event.organizationId() : 1L); // default to 1 if null
          
          Double amount = event.fee() != null ? event.fee() : 50.0;
          payment.setAmount(amount);
          payment.setTotalAmount(amount);
          
          payment.setStatus(PaymentStatus.PENDING);
          payment.setPaymentDate(Instant.now());
          payment.setDisease("Consultation");
          payment.setDescription("Facture automatique générée pour le rendez-vous du " + (event.dateTime() != null ? event.dateTime() : ""));
          
          paymentRepository.save(payment);
          log.info("Initialized billing record (PENDING) for appointmentId: {}", event.appointmentId());
        }
      }
    } catch (Exception e) {
      log.error("Failed to process billing event for appointmentId: " + event.appointmentId(), e);
    }
  }
}
