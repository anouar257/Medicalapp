package com.medical.practitioner.service;

import com.medical.practitioner.dto.SpecialtyDTO;
import com.medical.practitioner.entity.Specialty;
import com.medical.practitioner.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class SpecialtyService {

    private final SpecialtyRepository repository;

    public SpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    public List<SpecialtyDTO> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(Specialty::getLibelle))
                .map(s -> {
                    SpecialtyDTO dto = new SpecialtyDTO(s.getId(), s.getCode(), s.getLibelle());
                    dto.setDescription(s.getDescription());
                    return dto;
                })
                .toList();
    }

    @Transactional
    public Specialty createIfMissing(String code, String libelle) {
        return repository.findByCode(code).orElseGet(() -> {
            Specialty s = new Specialty(code, libelle);
            return repository.save(s);
        });
    }
}
