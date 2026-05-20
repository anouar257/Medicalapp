package com.medical.agenda.controller;

import com.medical.agenda.dto.GlobalStatsResponseDTO;
import com.medical.agenda.security.AgendaAccess;
import com.medical.agenda.security.AgendaProPrincipal;
import com.medical.agenda.service.GlobalStatsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class GlobalAdminController {

  private final GlobalStatsService globalStatsService;

  public GlobalAdminController(GlobalStatsService globalStatsService) {
    this.globalStatsService = globalStatsService;
  }

  @GetMapping("/global-stats")
  public GlobalStatsResponseDTO globalStats(
      Authentication authentication,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
    if (!(authentication.getPrincipal() instanceof AgendaProPrincipal principal)
        || !AgendaAccess.isPlatformAdmin(principal)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
    }
    return globalStatsService.build(authorization);
  }
}
