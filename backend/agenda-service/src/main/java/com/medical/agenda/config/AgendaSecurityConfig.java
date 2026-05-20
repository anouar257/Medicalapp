package com.medical.agenda.config;

import com.medical.agenda.security.AgendaJwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class AgendaSecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AgendaJwtAuthenticationFilter jwtFilter) throws Exception {
    http.cors(org.springframework.security.config.Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/error")
                    .permitAll()
                    .requestMatchers("/api/internal/**")
                    .permitAll()
                    .requestMatchers("/api/doctor-files/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/doctors/sync")
                    .permitAll()
                    .requestMatchers(HttpMethod.DELETE, "/api/doctors/sync/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
