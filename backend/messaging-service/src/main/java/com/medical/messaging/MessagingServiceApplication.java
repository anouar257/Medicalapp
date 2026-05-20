package com.medical.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.medical.messaging.client")
public class MessagingServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(MessagingServiceApplication.class, args);
  }
}
