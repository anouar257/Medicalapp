package com.medical.messaging.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class MessagingJwtAuthenticationFilter extends OncePerRequestFilter {

  private final DualJwtParser dualJwtParser;

  public MessagingJwtAuthenticationFilter(DualJwtParser dualJwtParser) {
    this.dualJwtParser = dualJwtParser;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);
    if (dualJwtParser.isExpired(token)) {
      filterChain.doFilter(request, response);
      return;
    }

    dualJwtParser
        .parse(token)
        .filter(p -> SecurityContextHolder.getContext().getAuthentication() == null)
        .ifPresent(
            principal -> {
              List<SimpleGrantedAuthority> authorities;
              if (principal instanceof MessagingPrincipal.MessagingPatient) {
                authorities = Collections.emptyList();
              } else if (principal instanceof MessagingPrincipal.MessagingPractitioner pr) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + pr.role()));
              } else {
                authorities = Collections.emptyList();
              }
              UsernamePasswordAuthenticationToken auth =
                  new UsernamePasswordAuthenticationToken(principal, null, authorities);
              auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(auth);
            });

    filterChain.doFilter(request, response);
  }
}
