package com.medical.messaging.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "patientMessaging",
    url = "${integration.patient-base-url}",
    path = "/api/internal/messages",
    configuration = InterServiceFeignConfig.class)
public interface PatientMessagingAuthorizationClient {

  @GetMapping("/can-represent")
  Map<String, Boolean> canRepresent(
      @RequestParam("ownerPatientId") Long ownerPatientId,
      @RequestParam("concernedPersonId") Long concernedPersonId);

  @PostMapping("/person-display-names")
  Map<Long, String> personDisplayNames(@RequestBody List<Long> ids);
}
