package com.medical.messaging.security;

/** Principal authentifié — patient (compte) ou utilisateur pro (JWT practitioner-service). */
public sealed interface MessagingPrincipal
    permits MessagingPrincipal.MessagingPatient, MessagingPrincipal.MessagingPractitioner {

  record MessagingPatient(Long patientId) implements MessagingPrincipal {}

  record MessagingPractitioner(Long userId, Long practitionerProfileId, String role)
      implements MessagingPrincipal {}
}
