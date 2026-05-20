package com.medical.practitioner.dto;

import com.medical.practitioner.entity.ConsultationLocation;
import com.medical.practitioner.entity.ContactUrgenceType;
import com.medical.practitioner.entity.HoraireOuverture;
import com.medical.practitioner.entity.JourSemaine;
import com.medical.practitioner.entity.ParkingType;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de lecture/écriture d'un lieu de consultation.
 */
public class ConsultationLocationDTO {

    private Long id;
    private Long practitionerId;
    private String nomEtablissement;
    private String adresse;
    private String ville;
    private String codePostal;
    private String pays;
    private String telephoneBureau;
    private String fax;
    private boolean ascenseur;
    private boolean entreeAccessible;
    private String etage;
    private ParkingType parking;
    private ContactUrgenceType contactUrgenceType;
    private String telephoneUrgence;
    private boolean actif;
    private List<HoraireDTO> horaires = new ArrayList<>();

    public static ConsultationLocationDTO fromEntity(ConsultationLocation l) {
        ConsultationLocationDTO dto = new ConsultationLocationDTO();
        dto.id = l.getId();
        if (l.getPractitioner() != null) {
            dto.practitionerId = l.getPractitioner().getId();
        }
        dto.nomEtablissement = l.getNomEtablissement();
        dto.adresse = l.getAdresse();
        dto.ville = l.getVille();
        dto.codePostal = l.getCodePostal();
        dto.pays = l.getPays();
        dto.telephoneBureau = l.getTelephoneBureau();
        dto.fax = l.getFax();
        dto.ascenseur = l.isAscenseur();
        dto.entreeAccessible = l.isEntreeAccessible();
        dto.etage = l.getEtage();
        dto.parking = l.getParking();
        dto.contactUrgenceType = l.getContactUrgenceType();
        dto.telephoneUrgence = l.getTelephoneUrgence();
        dto.actif = l.isActif();
        if (l.getHoraires() != null) {
            dto.horaires = l.getHoraires().stream().map(HoraireDTO::fromEntity).toList();
        }
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPractitionerId() { return practitionerId; }
    public void setPractitionerId(Long practitionerId) { this.practitionerId = practitionerId; }

    public String getNomEtablissement() { return nomEtablissement; }
    public void setNomEtablissement(String nomEtablissement) { this.nomEtablissement = nomEtablissement; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    public String getPays() { return pays; }
    public void setPays(String pays) { this.pays = pays; }

    public String getTelephoneBureau() { return telephoneBureau; }
    public void setTelephoneBureau(String telephoneBureau) { this.telephoneBureau = telephoneBureau; }

    public String getFax() { return fax; }
    public void setFax(String fax) { this.fax = fax; }

    public boolean isAscenseur() { return ascenseur; }
    public void setAscenseur(boolean ascenseur) { this.ascenseur = ascenseur; }

    public boolean isEntreeAccessible() { return entreeAccessible; }
    public void setEntreeAccessible(boolean entreeAccessible) { this.entreeAccessible = entreeAccessible; }

    public String getEtage() { return etage; }
    public void setEtage(String etage) { this.etage = etage; }

    public ParkingType getParking() { return parking; }
    public void setParking(ParkingType parking) { this.parking = parking; }

    public ContactUrgenceType getContactUrgenceType() { return contactUrgenceType; }
    public void setContactUrgenceType(ContactUrgenceType contactUrgenceType) { this.contactUrgenceType = contactUrgenceType; }

    public String getTelephoneUrgence() { return telephoneUrgence; }
    public void setTelephoneUrgence(String telephoneUrgence) { this.telephoneUrgence = telephoneUrgence; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public List<HoraireDTO> getHoraires() { return horaires; }
    public void setHoraires(List<HoraireDTO> horaires) { this.horaires = horaires; }

    public static class HoraireDTO {
        public Long id;
        public JourSemaine jour;
        public LocalTime heureDebut;
        public LocalTime heureFin;
        public boolean continu;

        public static HoraireDTO fromEntity(HoraireOuverture h) {
            HoraireDTO dto = new HoraireDTO();
            dto.id = h.getId();
            dto.jour = h.getJour();
            dto.heureDebut = h.getHeureDebut();
            dto.heureFin = h.getHeureFin();
            dto.continu = h.isContinu();
            return dto;
        }
    }
}
