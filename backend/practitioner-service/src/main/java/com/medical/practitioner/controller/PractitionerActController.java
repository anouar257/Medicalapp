package com.medical.practitioner.controller;

import com.medical.practitioner.dto.PractitionerActDTO;
import com.medical.practitioner.entity.PractitionerAct;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.repository.PractitionerActRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.security.AccessPolicies;
import com.medical.practitioner.service.PractitionerActService;
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
@RequestMapping("/api/pro/acts")
public class PractitionerActController {

    private final PractitionerActService service;
    private final PractitionerActRepository repository;
    private final PractitionerProfileRepository practitionerProfileRepository;

    public PractitionerActController(
            PractitionerActService service,
            PractitionerActRepository repository,
            PractitionerProfileRepository practitionerProfileRepository) {
        this.service = service;
        this.repository = repository;
        this.practitionerProfileRepository = practitionerProfileRepository;
    }

    @GetMapping("/by-practitioner/{practitionerId}")
    public List<PractitionerActDTO> list(@PathVariable Long practitionerId) {
        return service.findByPractitioner(practitionerId);
    }

    @PostMapping("/by-practitioner/{practitionerId}")
    public ResponseEntity<PractitionerActDTO> create(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long practitionerId,
            @RequestBody PractitionerActDTO body) {
        AccessPolicies.requireOwnerOrCabinetMember(user, practitionerId, practitionerProfileRepository);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(practitionerId, body));
    }

    @PutMapping("/{id}")
    public PractitionerActDTO update(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id,
            @RequestBody PractitionerActDTO body) {
        PractitionerAct act = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, act.getPractitioner());
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal ProUser user,
            @PathVariable Long id) {
        PractitionerAct act = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte introuvable"));
        AccessPolicies.requireOwnerOrCabinetMember(user, act.getPractitioner());
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
