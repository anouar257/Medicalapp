package com.medical.messaging.client;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Configuration Feign — ajoute le secret partagé pour les appels agenda / patient.
 *
 * <p>Référencée explicitement par {@code @FeignClient(configuration = ...)} (pas de scan
 * {@code @Configuration} pour éviter les doubles enregistrements).
 */
public class InterServiceFeignConfig {

  @Bean
  RequestInterceptor messagingSecretInterceptor(
      @Value("${integration.messaging-secret:}") String messagingSecret) {
    return template -> {
      if (messagingSecret != null && !messagingSecret.isBlank()) {
        template.header("X-Messaging-Secret", messagingSecret);
      }
    };
  }
}
