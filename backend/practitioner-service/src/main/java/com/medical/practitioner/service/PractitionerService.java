package com.medical.practitioner.service;

import com.medical.practitioner.dto.ConsultationLocationDTO;
import com.medical.practitioner.dto.PractitionerActDTO;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;

/**
 * Service de gestion des profils praticiens (mise à jour, vérifications, recherche).
 */
@Service
public class PractitionerService {

    private static final String PROFIL_INTROUVABLE = "Profil introuvable";

    private final PractitionerProfileRepository repository;
    private final SpecialtyRepository specialtyRepository;
    private final AgendaSyncService agendaSyncService;
    private final ConsultationLocationService consultationLocationService;
    private final PractitionerPhotoService practitionerPhotoService;
    private final PractitionerActService practitionerActService;

    public PractitionerService(PractitionerProfileRepository repository,
                               SpecialtyRepository specialtyRepository,
                               AgendaSyncService agendaSyncService,
                               ConsultationLocationService consultationLocationService,
                               PractitionerPhotoService practitionerPhotoService,
                               PractitionerActService practitionerActService) {
        this.repository = repository;
        this.specialtyRepository = specialtyRepository;
        this.agendaSyncService = agendaSyncService;
        this.consultationLocationService = consultationLocationService;
        this.practitionerPhotoService = practitionerPhotoService;
        this.practitionerActService = practitionerActService;
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
        return consultationLocationService.findActiveByPractitioner(practitionerId);
    }

    @Transactional(readOnly = true)
    public List<PractitionerActDTO> listPublicActs(Long practitionerId) {
        PractitionerProfile p = repository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Praticien introuvable"));
        if (!p.isDisponible()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil non publié");
        }
        return practitionerActService.findByPractitioner(practitionerId);
    }

    @Transactional(readOnly = true)
    public PractitionerProfileDTO findById(Long id) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROFIL_INTROUVABLE));
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
        String specNorm = normalizeLike(specialty);

        String nameLike = nameNorm != null ? "%" + nameNorm.toLowerCase() + "%" : null;
        String villeLike = villeNorm != null ? "%" + villeNorm.toLowerCase() + "%" : null;
        String specLike = specNorm != null ? "%" + specNorm.toLowerCase() + "%" : null;

        List<PractitionerProfile> profiles = repository.searchPublic(
                nameLike,
                villeLike,
                specLike
        );

        return profiles.stream().map(p -> toSearchResult(p, villeNorm, specNorm)).toList();
    }

    @Transactional(readOnly = true)
    public List<PractitionerSearchResultDTO> featuredPublic(int limit) {
        int safeLimit = Math.max(1, limit);
        return searchPublic(null, null, null).stream()
                .sorted(
                        Comparator.comparing(
                                        PractitionerSearchResultDTO::getGlobalRating,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(
                                        PractitionerSearchResultDTO::getReviewCount,
                                        Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(
                                        PractitionerSearchResultDTO::getNom,
                                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .limit(safeLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listPublicCities() {
        Set<String> cities = new LinkedHashSet<>();
        for (PractitionerProfile p : repository.searchPublic(null, null, null)) {
            if (p.getLieuxConsultation() != null) {
                for (ConsultationLocation location : p.getLieuxConsultation()) {
                    addCity(cities, location != null ? location.getVille() : null);
                }
            }
            if (p.getProUser() != null && p.getProUser().getOrganization() != null) {
                addCity(cities, p.getProUser().getOrganization().getVille());
            }
        }
        return new ArrayList<>(cities);
    }

    public PractitionerProfileDTO getPublicProfile(Long id) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Practitioner introuvable"));
        return PractitionerProfileDTO.fromEntity(p);
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

        dto.setVille(resolveCity(p, villeNorm));
        applySpecialties(p, specialtyNorm, dto);
        dto.setPhotoUrl(p.getPhotoUrl());
        dto.setAdresse(resolveMedicalOrganizationAddress(p));
        dto.setConsultationFee(resolveConsultationFee(p));
        dto.setGlobalRating(p.getGlobalRating());
        dto.setReviewCount(p.getReviewCount());

        boolean multiple = false;
        if (p.getLieuxConsultation() != null) {
            long activeCount = p.getLieuxConsultation().stream()
                    .filter(l -> l != null && l.isActif())
                    .count();
            multiple = activeCount > 1;
        }
        dto.setHasMultipleLocations(multiple);

        return dto;
    }

    private static String resolveCity(PractitionerProfile p, String villeNorm) {
        String city = "";
        if (p.getLieuxConsultation() != null) {
            city = findMatchingCity(p.getLieuxConsultation(), villeNorm);
        }
        if (city.isEmpty() && p.getProUser() != null && p.getProUser().getOrganization() != null) {
            String orgVille = p.getProUser().getOrganization().getVille();
            if (orgVille != null && !orgVille.isBlank()) {
                city = orgVille;
            }
        }
        return city;
    }

    private static String findMatchingCity(List<ConsultationLocation> locations, String villeNorm) {
        String fallback = "";
        for (ConsultationLocation l : locations) {
            if (l == null || l.getVille() == null || l.getVille().isBlank()) {
                continue;
            }
            String lVille = l.getVille();
            if (villeNorm != null && lVille.toLowerCase().contains(villeNorm.toLowerCase())) {
                return lVille;
            }
            if (fallback.isEmpty()) {
                fallback = lVille;
            }
        }
        return fallback;
    }

    private static void applySpecialties(PractitionerProfile p, String specialtyNorm, PractitionerSearchResultDTO dto) {
        String primaryCode = "";
        List<String> specLabels = new ArrayList<>();
        if (p.getSpecialites() != null) {
            for (Specialty s : p.getSpecialites()) {
                primaryCode = processSpecialty(s, specialtyNorm, specLabels, primaryCode);
            }
        }
        dto.setSpecialty(String.join(" / ", specLabels));
        dto.setPrimarySpecialtyCode(
                primaryCode != null && !primaryCode.isBlank() ? primaryCode : "MEDECINE_GENERALE");
    }

    private static String processSpecialty(Specialty s, String specialtyNorm, List<String> specLabels, String currentPrimaryCode) {
        if (s == null) {
            return currentPrimaryCode;
        }
        String lib = s.getLibelle() != null ? s.getLibelle() : "";
        String code = s.getCode() != null ? s.getCode() : "";
        if (!lib.isEmpty()) {
            specLabels.add(lib);
        }
        String primaryCode = currentPrimaryCode;
        if (primaryCode.isEmpty()) {
            primaryCode = code;
        }
        if (specialtyNorm != null && matchesSpecialty(lib, code, specialtyNorm)) {
            primaryCode = code;
        }
        return primaryCode;
    }

    private static boolean matchesSpecialty(String libelle, String code, String specialtyNorm) {
        String normLower = specialtyNorm.toLowerCase();
        return libelle.toLowerCase().contains(normLower) || code.toLowerCase().contains(normLower);
    }

    private static String resolveMedicalOrganizationAddress(PractitionerProfile p) {
        if (p.getProUser() != null && p.getProUser().getOrganization() != null) {
            String orgAdresse = p.getProUser().getOrganization().getAdresse();
            if (orgAdresse != null && !orgAdresse.isBlank()) {
                return orgAdresse.trim();
            }
        }
        return null;
    }

    private static BigDecimal resolveConsultationFee(PractitionerProfile p) {
        if (p.getConsultationFee() != null) {
            return p.getConsultationFee();
        }
        if (p.getLieuxConsultation() == null) {
            return null;
        }
        return p.getLieuxConsultation().stream()
                .filter(l -> l != null && l.isActif() && l.getConsultationFee() != null)
                .map(ConsultationLocation::getConsultationFee)
                .findFirst()
                .orElse(null);
    }

    private static void addCity(Set<String> cities, String rawCity) {
        if (rawCity == null) return;
        String city = rawCity.trim();
        if (city.isEmpty()) return;
        cities.add(city);
    }

    /**
     * Met à jour les champs du profil et resynchronise vers l'agenda-service.
     */
    @Transactional
    public PractitionerProfileDTO update(Long id, PractitionerProfileDTO body) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROFIL_INTROUVABLE));

        p.setCivilite(body.getCivilite());
        p.setTitre(body.getTitre());
        p.setSexe(body.getSexe());
        p.setDateNaissance(body.getDateNaissance());
        p.setStatut(body.getStatut());
        p.setLienYoutube(body.getLienYoutube());
        p.setSiteWeb(body.getSiteWeb());
        p.setBiographie(body.getBiographie());
        p.setPhotoUrl(body.getPhotoUrl());
        if (body.getColorCode() != null && !body.getColorCode().isBlank()) p.setColorCode(body.getColorCode());
        p.setConsultationFee(body.getConsultationFee());

        updateProUser(p, body);
        updateSpecialties(p, body);

        p = repository.save(p);
        agendaSyncService.syncPractitioner(p);
        return PractitionerProfileDTO.fromEntity(p);
    }

    private void updateProUser(PractitionerProfile p, PractitionerProfileDTO body) {
        if (body.getProUserId() != null && p.getProUser() != null) {
            if (body.getPrenom() != null && !body.getPrenom().isBlank()) {
                p.getProUser().setPrenom(body.getPrenom().trim());
            }
            if (body.getNom() != null && !body.getNom().isBlank()) {
                p.getProUser().setNom(body.getNom().trim());
            }
        }
    }

    private void updateSpecialties(PractitionerProfile p, PractitionerProfileDTO body) {
        if (body.getSpecialites() != null) {
            Set<Specialty> set = new HashSet<>();
            for (var dto : body.getSpecialites()) {
                if (dto.getId() != null) {
                    specialtyRepository.findById(dto.getId()).ifPresent(set::add);
                }
            }
            p.setSpecialites(set);
        }
    }

    @Transactional
    public PractitionerProfileDTO savePhoto(Long proUserId, MultipartFile file) {
        PractitionerProfile p = repository.findByProUserId(proUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profil praticien introuvable"));
        String previous = p.getPhotoUrl();
        String newUrl = practitionerPhotoService.store(p.getId(), file);
        practitionerPhotoService.deleteManagedIfPresent(previous);
        p.setPhotoUrl(newUrl);
        p = repository.save(p);
        agendaSyncService.syncPractitioner(p);
        return PractitionerProfileDTO.fromEntity(p);
    }

    @Transactional
    public PractitionerProfileDTO updateReviewSummary(Long practitionerId, Double globalRating, Integer reviewCount) {
        PractitionerProfile p = repository.findById(practitionerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROFIL_INTROUVABLE));
        p.setGlobalRating(globalRating);
        p.setReviewCount(reviewCount);
        p = repository.save(p);
        return PractitionerProfileDTO.fromEntity(p);
    }

    @Transactional
    public PractitionerProfileDTO setVerification(Long id, String type, VerificationStatus status, String docUrl) {
        PractitionerProfile p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROFIL_INTROUVABLE));

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
