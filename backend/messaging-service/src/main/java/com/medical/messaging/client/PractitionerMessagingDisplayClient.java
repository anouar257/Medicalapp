package com.medical.messaging.client;

import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "practitionerMessaging",
    url = "${integration.practitioner-base-url}",
    path = "/api/internal/messaging",
    configuration = InterServiceFeignConfig.class)
public interface PractitionerMessagingDisplayClient {

  @PostMapping("/practitioner-display-names")
  Map<Long, String> practitionerDisplayNames(@RequestBody List<Long> ids);
}
