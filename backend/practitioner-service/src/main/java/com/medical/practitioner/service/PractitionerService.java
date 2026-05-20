package com.medical.practitioner.service;

import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.dto.PractitionerSearchResultDTO;
import com.medical.practitioner.entity.ConsultationLocation;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.Specialty;
import com.medical.practitioner.entity.VerificationStatus;
import com.medical.practitioner.repository.PractitionerProfileRepository;
import com.medical.practitioner.repository.SpecialtyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

/**
 * Service de gestion des profils praticiens (mise à jour, vérifications, recherche).
 */
@Service
public class PractitionerService {

    private final PractitionerProfileRepository repository;
    private final SpecialtyRepository specialtyRepository;
    private final AgendaSyncService agendaSyncService;
    private final ConsultationLocationService consultationLocationService;

    public PractitionerService(PractitionerProfileRepository repository,
                               SpecialtyRepository specialtyRepository,
                               AgendaSyncService agendaSyncService,
                               ConsultationLocationService consultationLocationService) {
        this.repository = repository;
        this.specialtyRepository = specialtyRepository;
        this.agendaSyncService = agendaSyncService;
        this.consultationLocationService = consultationLocationService;
    }

    /**
     * Lieux de consultation d'un praticien visibles publiquement (profil {@code disponible}).
     * Utilisé pour la carte / choix « cabinet » dans le parcours patient.
     */
    @Transactional(readOnly = true)
    public List<ConsultationLocationDTO> listPublicLocations(Long practitionerId) {
        PractitionerProfile p = repository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Praticien introuvable"));
        if (!p.isDisponible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil non publié");
        }
        return consultationLocationService.findByPractitioner(practitionerId);
    }

    @Transactional(readOnly = true)
    public PractitionerProfileDTO findById(Long id) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));
        return PractitionerProfileDTO.fromEntity(p);
    }

    @Transactional(readOnly = true)
    public PractitionerProfileDTO findByProUserId(Long proUserId) {
        PractitionerProfile p = repository.findByProUserId(proUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil praticien introuvable"));
        return PractitionerProfileDTO.fromEntity(p);
    }

    @Transactional(readOnly = true)
    public List<PractitionerProfileDTO> findByOrganization(Long organizationId) {
        return repository.findByProUserOrganizationId(organizationId).stream()
                .map(PractitionerProfileDTO::fromEntity)
                .toList();
    }

    /** Liste complète des profils praticiens (administration plateforme). */
    @Transactional(readOnly = true)
    public List<PractitionerProfileDTO> findAllForPlatformAdmin() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(PractitionerProfile::getId))
                .map(PractitionerProfileDTO::fromEntity)
                .toList();
    }

    /**
     * Recherche “public” pour la Landing Page (typeahead).
     *
     * <p>Filtres optionnels :
     * <ul>
     *   <li>{@code name} : prénom / nom</li>
     *   <li>{@code ville} : ville du lieu de consultation</li>
     *   <li>{@code specialty} : libellé (ou code) de la spécialité</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public List<PractitionerSearchResultDTO> searchPublic(String name, String ville, String specialty) {
        String nameNorm = normalizeLike(name);
        String villeNorm = normalizeLike(ville);
        String specialtyNorm = normalizeLike(specialty);

        List<PractitionerProfile> profiles = repository.searchPublic(
                nameNorm,
                villeNorm,
                specialtyNorm
        );

        return profiles.stream().map(p -> toSearchResult(p, villeNorm, specialtyNorm)).toList();
    }

    private static String normalizeLike(String raw) {
        if (raw == null) return null;
        String x = raw.trim();
        if (x.isEmpty()) return null;
        return x;
    }

    private static PractitionerSearchResultDTO toSearchResult(
            PractitionerProfile p,
            String villeNorm,
            String specialtyNorm
    ) {
        PractitionerSearchResultDTO dto = new PractitionerSearchResultDTO();
        dto.setPractitionerId(p.getId());

        String prenom = p.getProUser() != null ? p.getProUser().getPrenom() : "";
        String nom = p.getProUser() != null ? p.getProUser().getNom() : "";
        dto.setNom((prenom + " " + nom).trim());

        // Ville : préférer un match sur le filtre, sinon prendre la 1ère ville non vide.
        String city = "";
        if (p.getLieuxConsultation() != null) {
            for (ConsultationLocation l : p.getLieuxConsultation()) {
                if (l == null) continue;
                String lVille = l.getVille();
                if (lVille == null || lVille.isBlank()) continue;

                if (villeNorm != null && lVille.toLowerCase().contains(villeNorm.toLowerCase())) {
                    city = lVille;
                    break;
                }
                if (city.isEmpty()) city = lVille;
            }
        }
        dto.setVille(city);

        // Spécialité : préférer un match sur le filtre, sinon prendre la 1ère spécialité.
        String specLabel = "";
        String specCode = "";
        if (p.getSpecialites() != null) {
            for (Specialty s : p.getSpecialites()) {
                if (s == null) continue;
                String lib = s.getLibelle();
                if (lib == null) lib = "";
                String code = s.getCode();
                if (code == null) code = "";

                if (specialtyNorm != null) {
                    boolean matches =
                            lib.toLowerCase().contains(specialtyNorm.toLowerCase()) ||
                                    code.toLowerCase().contains(specialtyNorm.toLowerCase());
                    if (matches) {
                        specLabel = lib;
                        specCode = code;
                        break;
                    }
                }

                if (specLabel.isEmpty()) {
                    specLabel = lib;
                    specCode = code;
                }
            }
        }
        dto.setSpecialty(specLabel);
        dto.setPrimarySpecialtyCode(
                specCode != null && !specCode.isBlank() ? specCode : "MEDECINE_GENERALE");
        return dto;
    }

    /**
     * Met à jour les champs du profil et resynchronise vers l'agenda-service.
     */
    @Transactional
    public PractitionerProfileDTO update(Long id, PractitionerProfileDTO body) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));

        if (body.getCivilite() != null) p.setCivilite(body.getCivilite());
        if (body.getTitre() != null) p.setTitre(body.getTitre());
        if (body.getSexe() != null) p.setSexe(body.getSexe());
        if (body.getDateNaissance() != null) p.setDateNaissance(body.getDateNaissance());
        if (body.getStatut() != null) p.setStatut(body.getStatut());
        if (body.getLienYoutube() != null) p.setLienYoutube(body.getLienYoutube());
        if (body.getSiteWeb() != null) p.setSiteWeb(body.getSiteWeb());
        if (body.getBiographie() != null) p.setBiographie(body.getBiographie());
        if (body.getPhotoUrl() != null) p.setPhotoUrl(body.getPhotoUrl());
        if (body.getColorCode() != null && !body.getColorCode().isBlank()) p.setColorCode(body.getColorCode());

        if (body.getProUserId() != null && p.getProUser() != null) {
            if (body.getPrenom() != null && !body.getPrenom().isBlank()) {
                p.getProUser().setPrenom(body.getPrenom().trim());
            }
            if (body.getNom() != null && !body.getNom().isBlank()) {
                p.getProUser().setNom(body.getNom().trim());
            }
        }

        if (body.getSpecialites() != null) {
            Set<Specialty> set = new HashSet<>();
            for (var dto : body.getSpecialites()) {
                if (dto.getId() != null) {
                    specialtyRepository.findById(dto.getId()).ifPresent(set::add);
                }
            }
            p.setSpecialites(set);
        }

        p = repository.save(p);
        agendaSyncService.syncPractitioner(p);
        return PractitionerProfileDTO.fromEntity(p);
    }

    @Transactional
    public PractitionerProfileDTO setVerification(Long id, String type, VerificationStatus status, String docUrl) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil introuvable"));

        if ("identite".equalsIgnoreCase(type)) {
            p.setVerifIdentiteStatus(status);
            if (docUrl != null) p.setDocIdentiteUrl(docUrl);
        } else if ("droit-exercer".equalsIgnoreCase(type) || "droit_exercer".equalsIgnoreCase(type)) {
            p.setVerifDroitExercerStatus(status);
            if (docUrl != null) p.setDocDroitExercerUrl(docUrl);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de vérification inconnu : " + type);
        }
        p = repository.save(p);
        return PractitionerProfileDTO.fromEntity(p);
    }
}
