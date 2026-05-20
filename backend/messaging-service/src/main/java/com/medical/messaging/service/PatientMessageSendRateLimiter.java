package com.medical.messaging.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Limite simple en mémoire : au plus {@value #MAX_SENDS_PER_WINDOW} envois par fenêtre de
 * {@value #WINDOW_MS} ms par identifiant de compte patient (expéditeur).
 */
@Component
public class PatientMessageSendRateLimiter {

  static final int MAX_SENDS_PER_WINDOW = 5;
  static final long WINDOW_MS = 60_000L;

  private final ConcurrentHashMap<Long, List<Long>> timestampsByPatientId = new ConcurrentHashMap<>();

  /**
   * Enregistre un envoi pour ce patient ou lève {@link HttpStatus#TOO_MANY_REQUESTS} si la limite
   * est dépassée.
   */
  public void acquireOrThrow(Long patientAccountId) {
    timestampsByPatientId.compute(
        patientAccountId,
        (id, times) -> {
          long now = System.currentTimeMillis();
          List<Long> list = times == null ? new ArrayList<>() : times;
          list.removeIf(t -> t < now - WINDOW_MS);
          if (list.size() >= MAX_SENDS_PER_WINDOW) {
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "Limite d'envoi atteinte : maximum 5 messages par minute pour un compte patient. Réessayez dans quelques instants.");
          }
          list.add(now);
          return list;
        });
  }
}
