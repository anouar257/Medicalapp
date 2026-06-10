package com.medical.messaging.client;

import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "agendaMessaging",
    url = "${integration.agenda-base-url}",
    path = "/api/internal/messages",
    configuration = InterServiceFeignConfig.class)
public interface AgendaMessagingClient {

  @GetMapping("/has-relationship")
  Map<String, Boolean> hasRelationship(
      @RequestParam("patientId") Long patientId,
      @RequestParam("externalPractitionerId") Long externalPractitionerId,
      @RequestParam("completedOnly") boolean completedOnly);
}
