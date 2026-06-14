package com.medical.practitioner.controller;

import com.medical.practitioner.dto.DiplomaDTO;
import com.medical.practitioner.entity.Diploma;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.repository.DiplomaRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.security.AccessPolicies;
import com.medical.practitioner.service.DiplomaService;
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
@RequestMapping("/api/pro/diplomas")
public class DiplomaController {

    private final DiplomaService service;
    private final DiplomaRepository repository;
    private final PractitionerProfileRepository practitionerProfileRepository;

    public DiplomaController(
            DiplomaService service,
            DiplomaRepository repository,
            PractitionerProfileRepository practitionerProfileRepository) {
        this.service = service;
        this.repository = repository;
        this.practitionerProfileRepository = practitionerProfileRepository;
    }

    @GetMapping("/by-practitioner/{practitionerId}")
    public List<DiplomaDTO> list(@PathVariable Long practitionerId) {
        return service.findByPractitioner(practitionerId);
    }

    @PostMapping("/by-practitioner/{practitionerId}")
    public ResponseEntity<DiplomaDTO> create(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long practitionerId,
            @RequestBody DiplomaDTO body) {
        AccessPolicies.requireOwnerOrCabinetMember(user, practitionerId, practitionerProfileRepository);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(practitionerId, body));
    }

    @PutMapping("/{id}")
    public DiplomaDTO update(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id,
            @RequestBody DiplomaDTO body) {
        Diploma diploma = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diplôme introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, diploma.getPractitioner());
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id) {
        Diploma diploma = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diplôme introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, diploma.getPractitioner());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
