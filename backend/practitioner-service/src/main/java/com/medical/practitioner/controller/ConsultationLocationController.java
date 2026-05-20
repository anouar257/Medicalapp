package com.medical.practitioner.controller;

import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.service.ConsultationLocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pro/locations")
public class ConsultationLocationController {

    private final ConsultationLocationService service;

    public ConsultationLocationController(ConsultationLocationService service) {
        this.service = service;
    }

    @GetMapping("/by-practitioner/{practitionerId}")
    public List<ConsultationLocationDTO> list(@PathVariable Long practitionerId) {
        return service.findByPractitioner(practitionerId);
    }

    @PostMapping("/by-practitioner/{practitionerId}")
    public ResponseEntity<ConsultationLocationDTO> create(@PathVariable Long practitionerId,
                                                          @RequestBody ConsultationLocationDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(practitionerId, body));
    }

    @PutMapping("/{id}")
    public ConsultationLocationDTO update(@PathVariable Long id, @RequestBody ConsultationLocationDTO body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
