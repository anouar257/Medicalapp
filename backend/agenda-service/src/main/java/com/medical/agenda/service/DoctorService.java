package com.medical.agenda.service;

import com.medical.agenda.dto.DoctorDTO;
import com.medical.agenda.entity.Doctor;
import com.medical.agenda.repository.AppointmentRepository;
import com.medical.agenda.repository.DoctorRepository;
import com.medical.agenda.security.AgendaAccess;
import com.medical.agenda.security.AgendaProPrincipal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DoctorService {

  /**
   * Fichier statique du frontend Angular ({@code public/assets/doctors/placeholder.svg}), servi sous
   * {@code /assets/doctors/placeholder.svg}.
   */
  public static final String DEFAULT_DOCTOR_PHOTO_ASSET = "/assets/doctors/placeholder.svg";

  private final DoctorRepository doctorRepository;
  private final AppointmentRepository appointmentRepository;
  private final DoctorPhotoFileService doctorPhotoFileService;

  public DoctorService(
      DoctorRepository doctorRepository,
      AppointmentRepository appointmentRepository,
      DoctorPhotoFileService doctorPhotoFileService) {
    this.doctorRepository = doctorRepository;
    this.appointmentRepository = appointmentRepository;
    this.doctorPhotoFileService = doctorPhotoFileService;
  }

  @Transactional(readOnly = true)
  public List<DoctorDTO> findAll() {
    Map<Long, Long> counts =
        appointmentRepository.countAppointmentsGroupedByDoctorId().stream()
            .collect(
                Collectors.toMap(
                    row -> (Long) row[0],
                    row -> (Long) row[1],
                    (a, b) -> a,
                    java.util.HashMap::new));

    AgendaProPrincipal principal = currentPrincipal();
    List<Doctor> doctors;
    if (principal == null || AgendaAccess.isPlatformAdmin(principal)) {
      doctors = doctorRepository.findAll();
    } else if (principal.organizationId() == null) {
      // Pro sans cabinet rattaché : aucun médecin à exposer.
      return List.of();
    } else {
      doctors = doctorRepository.findByOrganizationId(principal.organizationId());
    }

    return doctors.stream()
        .map(d -> toDto(d, counts.getOrDefault(d.getId(), 0L)))
        .toList();
  }

  private static AgendaProPrincipal currentPrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    Object p = auth.getPrincipal();
    return p instanceof AgendaProPrincipal pp ? pp : null;
  }

  @Transactional(readOnly = true)
  public DoctorDTO findByExternalPractitionerId(Long externalPractitionerId) {
    Doctor d =
        doctorRepository
            .findByExternalPractitionerId(externalPractitionerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin agenda introuvable."));
    return toDto(d, appointmentRepository.countByDoctor_Id(d.getId()));
  }

  /** Crée un médecin (corps JSON : {@code name} obligatoire ; {@code colorCode}, {@code photoUrl} optionnels). */
  @Transactional
  public DoctorDTO create(DoctorDTO input) {
    if (input.getName() == null || input.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du médecin est obligatoire.");
    }
    Doctor d = new Doctor();
    String name = input.getName().trim();
    d.setName(name);
    String color = input.getColorCode();
    String resolvedColor = color != null && !color.isBlank() ? color.trim() : "#64748b";
    d.setColorCode(resolvedColor);

    String photo = input.getPhotoUrl();
    String resolvedPhoto =
        photo != null && !photo.isBlank() ? photo.trim() : DEFAULT_DOCTOR_PHOTO_ASSET;
    d.setPhotoUrl(resolvedPhoto);
    if (input.getSpecialty() != null && !input.getSpecialty().isBlank()) {
      d.setSpecialty(input.getSpecialty().trim());
    }
    if (input.getSpecialtyCode() != null && !input.getSpecialtyCode().isBlank()) {
      d.setSpecialtyCode(input.getSpecialtyCode().trim().toUpperCase());
    }

    if (input.getOrganizationId() != null) {
      d.setOrganizationId(input.getOrganizationId());
    } else {
      // Rattache automatiquement le médecin au cabinet du compte pro courant
      // pour cloisonner l'agenda entre cabinets.
      AgendaProPrincipal principal = currentPrincipal();
      if (principal != null && principal.organizationId() != null) {
        d.setOrganizationId(principal.organizationId());
      }
    }

    d = doctorRepository.save(d);
    return toDto(d, 0L);
  }

  /**
   * Met à jour le nom, la couleur et/ou la photo. L’{@code id} du corps JSON est ignoré : l’identifiant
   * officiel est celui du chemin ({@code /api/doctors/{id}}).
   */
  @Transactional
  public DoctorDTO update(Long id, DoctorDTO input) {
    Doctor d =
        doctorRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin inconnu."));
    if (input.getName() == null || input.getName().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom du médecin est obligatoire.");
    }
    d.setName(input.getName().trim());
    String color = input.getColorCode();
    if (color != null && !color.isBlank()) {
      d.setColorCode(color.trim());
    }
    String photo = input.getPhotoUrl();
    if (photo != null && !photo.isBlank()) {
      String prev = d.getPhotoUrl();
      String next = photo.trim();
      if (!next.equals(prev)) {
        doctorPhotoFileService.deleteManagedIfPresent(prev);
      }
      d.setPhotoUrl(next);
    } else if (d.getPhotoUrl() == null || d.getPhotoUrl().isBlank()) {
      d.setPhotoUrl(DEFAULT_DOCTOR_PHOTO_ASSET);
    }
    if (input.getSpecialty() != null) {
      d.setSpecialty(input.getSpecialty().isBlank() ? null : input.getSpecialty().trim());
    }
    if (input.getSpecialtyCode() != null) {
      d.setSpecialtyCode(
          input.getSpecialtyCode().isBlank() ? null : input.getSpecialtyCode().trim().toUpperCase());
    }
    if (input.getOrganizationId() != null) {
      d.setOrganizationId(input.getOrganizationId());
    }
    d = doctorRepository.save(d);
    return toDto(d, appointmentRepository.countByDoctor_Id(d.getId()));
  }

  /**
   * Supprime un médecin uniquement s’il n’existe aucun rendez-vous associé ; sinon erreur {@code 409}.
   */
  @Transactional
  public void delete(Long id) {
    Doctor d =
        doctorRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin inconnu."));
    long appointments = appointmentRepository.countByDoctor_Id(id);
    if (appointments > 0) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Impossible de supprimer ce médecin : "
              + appointments
              + " rendez-vous encore liés. Supprimez ou déplacez d’abord ces rendez-vous.");
    }
    doctorPhotoFileService.deleteManagedIfPresent(d.getPhotoUrl());
    doctorRepository.delete(d);
  }

  /**
   * Crée ou met à jour un Doctor à partir des données envoyées par le practitioner-service.
   *
   * <p>Le mapping se fait par {@code externalPractitionerId} (ID du {@code PractitionerProfile}
   * dans le practitioner-service). Aucun rendez-vous n'est touché — on ne fait que
   * synchroniser les champs d'affichage (nom, couleur, photo, spécialité).
   */
  @Transactional
  public DoctorDTO syncFromPractitioner(
      Long externalPractitionerId,
      String name,
      String colorCode,
      String photoUrl,
      String specialty,
      String specialtyCode,
      Long organizationId) {
    if (externalPractitionerId == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "externalPractitionerId est obligatoire pour la synchronisation.");
    }
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Le nom du praticien est obligatoire.");
    }

    Doctor d =
        doctorRepository
            .findByExternalPractitionerId(externalPractitionerId)
            .orElseGet(Doctor::new);

    d.setExternalPractitionerId(externalPractitionerId);
    d.setName(name.trim());
    d.setColorCode(colorCode != null && !colorCode.isBlank() ? colorCode.trim() : "#0ea5e9");
    d.setPhotoUrl(
        photoUrl != null && !photoUrl.isBlank() ? photoUrl.trim() : DEFAULT_DOCTOR_PHOTO_ASSET);
    d.setSpecialty(
        specialty != null && !specialty.isBlank() ? specialty.trim() : "Médecine générale");
    if (specialtyCode != null && !specialtyCode.isBlank()) {
      d.setSpecialtyCode(specialtyCode.trim().toUpperCase());
    } else {
      d.setSpecialtyCode(null);
    }
    d.setOrganizationId(organizationId);

    d = doctorRepository.save(d);
    return toDto(
        d, d.getId() != null ? appointmentRepository.countByDoctor_Id(d.getId()) : 0L);
  }

  /**
   * Désynchronise un praticien : tente de supprimer le Doctor lié si aucun rendez-vous n'existe.
   * Sans rendez-vous, suppression effective ; sinon, le Doctor est conservé pour préserver
   * l'historique (mais perd son lien externe).
   */
  @Transactional
  public void unsyncByExternalPractitionerId(Long externalPractitionerId) {
    if (externalPractitionerId == null) return;
    doctorRepository
        .findByExternalPractitionerId(externalPractitionerId)
        .ifPresent(
            d -> {
              long appts = appointmentRepository.countByDoctor_Id(d.getId());
              if (appts == 0) {
                doctorPhotoFileService.deleteManagedIfPresent(d.getPhotoUrl());
                doctorRepository.delete(d);
              } else {
                d.setExternalPractitionerId(null);
                doctorRepository.save(d);
              }
            });
  }

  /** Enregistre un portrait uploadé et met à jour l’URL en base. */
  @Transactional
  public DoctorDTO savePhoto(Long id, MultipartFile file) {
    Doctor d =
        doctorRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Médecin inconnu."));
    String previous = d.getPhotoUrl();
    String newUrl = doctorPhotoFileService.store(id, file);
    doctorPhotoFileService.deleteManagedIfPresent(previous);
    d.setPhotoUrl(newUrl);
    d = doctorRepository.save(d);
    return toDto(d, appointmentRepository.countByDoctor_Id(d.getId()));
  }

  private DoctorDTO toDto(Doctor d, long appointmentCount) {
    DoctorDTO dto = new DoctorDTO();
    dto.setId(d.getId());
    dto.setName(d.getName());
    dto.setColorCode(d.getColorCode());
    dto.setPhotoUrl(
        d.getPhotoUrl() != null && !d.getPhotoUrl().isBlank()
            ? d.getPhotoUrl()
            : DEFAULT_DOCTOR_PHOTO_ASSET);
    dto.setSpecialty(d.getSpecialty());
    dto.setSpecialtyCode(d.getSpecialtyCode());
    dto.setAppointmentCount(appointmentCount);
    dto.setExternalPractitionerId(d.getExternalPractitionerId());
    dto.setOrganizationId(d.getOrganizationId());
    return dto;
  }
}
