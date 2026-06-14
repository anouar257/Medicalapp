package com.medical.practitioner.controller;

import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.entity.ConsultationLocation;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.repository.ConsultationLocationRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.security.AccessPolicies;
import com.medical.practitioner.service.ConsultationLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/pro/locations")
public class ConsultationLocationController {

    private final ConsultationLocationService service;
    private final ConsultationLocationRepository repository;
    private final PractitionerProfileRepository practitionerProfileRepository;

    public ConsultationLocationController(
            ConsultationLocationService service,
            ConsultationLocationRepository repository,
            PractitionerProfileRepository practitionerProfileRepository) {
        this.service = service;
        this.repository = repository;
        this.practitionerProfileRepository = practitionerProfileRepository;
    }

    @GetMapping("/by-practitioner/{practitionerId}")
    public List<ConsultationLocationDTO> list(@PathVariable Long practitionerId) {
        return service.findByPractitioner(practitionerId);
    }

    @PostMapping("/by-practitioner/{practitionerId}")
    public ResponseEntity<ConsultationLocationDTO> create(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long practitionerId,
            @RequestBody ConsultationLocationDTO body) {
        AccessPolicies.requireOwnerOrCabinetMember(user, practitionerId, practitionerProfileRepository);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(practitionerId, body));
    }

    @PutMapping("/{id}")
    public ConsultationLocationDTO update(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id,
            @RequestBody ConsultationLocationDTO body) {
        ConsultationLocation loc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, loc.getPractitioner());
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id) {
        ConsultationLocation loc = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, loc.getPractitioner());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
