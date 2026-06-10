package com.medical.practitioner.service;

import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.entity.ConsultationLocation;
import com.medical.practitioner.entity.HoraireOuverture;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.repository.ConsultationLocationRepository;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConsultationLocationService {

    private final ConsultationLocationRepository repository;
    private final PractitionerProfileRepository practitionerRepository;

    public ConsultationLocationService(ConsultationLocationRepository repository,
                                       PractitionerProfileRepository practitionerRepository) {
        this.repository = repository;
        this.practitionerRepository = practitionerRepository;
    }

    @Transactional(readOnly = true)
    public List<ConsultationLocationDTO> findByPractitioner(Long practitionerId) {
        return repository.findByPractitionerId(practitionerId).stream()
                .map(ConsultationLocationDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConsultationLocationDTO> findActiveByPractitioner(Long practitionerId) {
        return repository.findByPractitionerIdAndActifTrue(practitionerId).stream()
                .map(ConsultationLocationDTO::fromEntity)
                .toList();
    }

    @Transactional
    public ConsultationLocationDTO create(Long practitionerId, ConsultationLocationDTO body) {
        PractitionerProfile prat = practitionerRepository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Praticien introuvable"));

        ConsultationLocation l = applyToEntity(new ConsultationLocation(), body);
        l.setPractitioner(prat);

        l.setHoraires(buildHoraires(body, l));
        l = repository.save(l);
        return ConsultationLocationDTO.fromEntity(l);
    }

    @Transactional
    public ConsultationLocationDTO update(Long id, ConsultationLocationDTO body) {
        ConsultationLocation l = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu introuvable"));
        applyToEntity(l, body);

        l.getHoraires().clear();
        l.getHoraires().addAll(buildHoraires(body, l));

        l = repository.save(l);
        return ConsultationLocationDTO.fromEntity(l);
    }

    @Transactional
    public void delete(Long id) {
        ConsultationLocation l = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lieu introuvable"));
        repository.delete(l);
    }

    private ConsultationLocation applyToEntity(ConsultationLocation l, ConsultationLocationDTO body) {
        l.setNomEtablissement(body.getNomEtablissement());
        l.setAdresse(body.getAdresse());
        l.setVille(body.getVille());
        l.setCodePostal(body.getCodePostal());
        l.setPays(body.getPays());
        l.setTelephoneBureau(body.getTelephoneBureau());
        l.setFax(body.getFax());
        l.setConsultationFee(body.getConsultationFee());
        l.setAscenseur(body.isAscenseur());
        l.setEntreeAccessible(body.isEntreeAccessible());
        l.setEtage(body.getEtage());
        l.setParking(body.getParking());
        l.setContactUrgenceType(body.getContactUrgenceType());
        l.setTelephoneUrgence(body.getTelephoneUrgence());
        l.setActif(body.isActif());
        return l;
    }

    private List<HoraireOuverture> buildHoraires(ConsultationLocationDTO body, ConsultationLocation lieu) {
        List<HoraireOuverture> out = new ArrayList<>();
        if (body.getHoraires() == null) return out;
        for (var h : body.getHoraires()) {
            if (h.getJour() == null || h.getHeureDebut() == null || h.getHeureFin() == null) continue;
            HoraireOuverture entity = new HoraireOuverture();
            entity.setLieu(lieu);
            entity.setJour(h.getJour());
            entity.setHeureDebut(h.getHeureDebut());
            entity.setHeureFin(h.getHeureFin());
            entity.setContinu(h.isContinu());
            out.add(entity);
        }
        return out;
    }
}
