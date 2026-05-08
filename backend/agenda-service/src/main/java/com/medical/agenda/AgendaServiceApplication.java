package com.medical.agenda;

import com.medical.agenda.config.AgendaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AgendaProperties.class})
public class AgendaServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AgendaServiceApplication.class, args);
  }
}
