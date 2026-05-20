package com.medical.practitioner.controller;

import com.medical.practitioner.dto.DiplomaDTO;
import com.medical.practitioner.service.DiplomaService;
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
@RequestMapping("/api/pro/diplomas")
public class DiplomaController {

    private final DiplomaService service;

    public DiplomaController(DiplomaService service) {
        this.service = service;
    }

    @GetMapping("/by-practitioner/{practitionerId}")
    public List<DiplomaDTO> list(@PathVariable Long practitionerId) {
        return service.findByPractitioner(practitionerId);
    }

    @PostMapping("/by-practitioner/{practitionerId}")
    public ResponseEntity<DiplomaDTO> create(@PathVariable Long practitionerId,
                                             @RequestBody DiplomaDTO body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(practitionerId, body));
    }

    @PutMapping("/{id}")
    public DiplomaDTO update(@PathVariable Long id, @RequestBody DiplomaDTO body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
