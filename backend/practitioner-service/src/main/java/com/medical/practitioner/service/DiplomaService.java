package com.medical.practitioner.service;

import com.medical.practitioner.dto.DiplomaDTO;
import com.medical.practitioner.entity.Diploma;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.repository.DiplomaRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DiplomaService {

    private final DiplomaRepository repository;
    private final PractitionerProfileRepository practitionerRepository;

    public DiplomaService(DiplomaRepository repository, PractitionerProfileRepository practitionerRepository) {
        this.repository = repository;
        this.practitionerRepository = practitionerRepository;
    }

    @Transactional(readOnly = true)
    public List<DiplomaDTO> findByPractitioner(Long practitionerId) {
        return repository.findByPractitionerId(practitionerId).stream()
                .map(DiplomaDTO::fromEntity)
                .toList();
    }

    @Transactional
    public DiplomaDTO create(Long practitionerId, DiplomaDTO body) {
        PractitionerProfile prat = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Praticien introuvable"));
        Diploma d = applyToEntity(new Diploma(), body);
        d.setPractitioner(prat);
        d = repository.save(d);
        return DiplomaDTO.fromEntity(d);
    }

    @Transactional
    public DiplomaDTO update(Long id, DiplomaDTO body) {
        Diploma d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diplôme introuvable"));
        applyToEntity(d, body);
        d = repository.save(d);
        return DiplomaDTO.fromEntity(d);
    }

    @Transactional
    public void delete(Long id) {
        Diploma d = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Diplôme introuvable"));
        repository.delete(d);
    }

    private Diploma applyToEntity(Diploma d, DiplomaDTO body) {
        if (body.getIntitule() != null) d.setIntitule(body.getIntitule());
        if (body.getEtablissement() != null) d.setEtablissement(body.getEtablissement());
        if (body.getAnneeObtention() != null) d.setAnneeObtention(body.getAnneeObtention());
        if (body.getDateObtention() != null) d.setDateObtention(body.getDateObtention());
        if (body.getType() != null) d.setType(body.getType());
        if (body.getDocumentUrl() != null) d.setDocumentUrl(body.getDocumentUrl());
        return d;
    }
}
