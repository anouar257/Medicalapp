package com.medical.practitioner.config;

import com.medical.practitioner.repository.PractitionerActRepository;
import com.medical.practitioner.service.AgendaActSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgendaActSyncBootstrap {

  @Bean
  CommandLineRunner syncExistingPractitionerActs(
      PractitionerActRepository practitionerActRepository, AgendaActSyncService agendaActSyncService) {
    return args -> practitionerActRepository.findAllWithPractitioner().forEach(agendaActSyncService::syncAct);
  }
}
