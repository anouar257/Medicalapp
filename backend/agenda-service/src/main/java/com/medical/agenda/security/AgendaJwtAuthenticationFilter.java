package com.medical.agenda.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AgendaJwtAuthenticationFilter extends OncePerRequestFilter {

  private final AgendaDualJwtParser dualJwtParser;

  public AgendaJwtAuthenticationFilter(AgendaDualJwtParser dualJwtParser) {
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

    if (SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response);
      return;
    }

    dualJwtParser
        .parsePrincipal(token)
        .ifPresent(
            principal -> {
              List<SimpleGrantedAuthority> authorities;
              Object userPrincipal;
              if (principal instanceof AgendaPatientPrincipal) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_PATIENT"));
                userPrincipal = principal;
              } else if (principal instanceof AgendaProPrincipal p) {
                authorities = List.of(new SimpleGrantedAuthority("ROLE_" + p.role()));
                userPrincipal = p;
              } else {
                return;
              }
              UsernamePasswordAuthenticationToken authToken =
                  new UsernamePasswordAuthenticationToken(userPrincipal, null, authorities);
              authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
              SecurityContextHolder.getContext().setAuthentication(authToken);
            });

    filterChain.doFilter(request, response);
  }
}
