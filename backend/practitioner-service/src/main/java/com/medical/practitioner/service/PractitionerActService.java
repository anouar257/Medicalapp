package com.medical.practitioner.service;

import com.medical.practitioner.dto.PractitionerActDTO;
import com.medical.practitioner.entity.PractitionerAct;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.repository.PractitionerActRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PractitionerActService {

    private final PractitionerActRepository repository;
    private final PractitionerProfileRepository practitionerRepository;
    private final AgendaActSyncService agendaActSyncService;

    public PractitionerActService(PractitionerActRepository repository,
                                  PractitionerProfileRepository practitionerRepository,
                                  AgendaActSyncService agendaActSyncService) {
        this.repository = repository;
        this.practitionerRepository = practitionerRepository;
        this.agendaActSyncService = agendaActSyncService;
    }

    @Transactional(readOnly = true)
    public List<PractitionerActDTO> findByPractitioner(Long practitionerId) {
        return repository.findByPractitionerId(practitionerId).stream()
                .map(PractitionerActDTO::fromEntity)
                .toList();
    }

    @Transactional
    public PractitionerActDTO create(Long practitionerId, PractitionerActDTO body) {
        PractitionerProfile prat = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Praticien introuvable"));
        PractitionerAct act = applyToEntity(new PractitionerAct(), body);
        act.setPractitioner(prat);
        act = repository.save(act);
        agendaActSyncService.syncAct(act);
        return PractitionerActDTO.fromEntity(act);
    }

    @Transactional
    public PractitionerActDTO update(Long id, PractitionerActDTO body) {
        PractitionerAct act = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte introuvable"));
        applyToEntity(act, body);
        act = repository.save(act);
        agendaActSyncService.syncAct(act);
        return PractitionerActDTO.fromEntity(act);
    }

    @Transactional
    public void delete(Long id) {
        PractitionerAct act = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Acte introuvable"));
        Long practitionerId = act.getPractitioner() != null ? act.getPractitioner().getId() : null;
        repository.delete(act);
        agendaActSyncService.deactivateAct(practitionerId, act.getId());
    }

    private PractitionerAct applyToEntity(PractitionerAct act, PractitionerActDTO body) {
        act.setName(body.getName());
        act.setDurationMinutes(body.getDurationMinutes());
        act.setPrice(body.getPrice());
        act.setPriceVariable(body.isPriceVariable());
        return act;
    }
}
