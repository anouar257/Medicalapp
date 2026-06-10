package com.medical.practitioner.dto;

import com.medical.practitioner.entity.ConsultationLocation;
import com.medical.practitioner.entity.ContactUrgenceType;
import com.medical.practitioner.entity.HoraireOuverture;
import com.medical.practitioner.entity.JourSemaine;
import com.medical.practitioner.entity.ParkingType;

import java.math.BigDecimal;
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
    private BigDecimal consultationFee;
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
        dto.setId(l.getId());
        if (l.getPractitioner() != null) {
            dto.setPractitionerId(l.getPractitioner().getId());
        }
        dto.setNomEtablissement(l.getNomEtablissement());
        dto.setAdresse(l.getAdresse());
        dto.setVille(l.getVille());
        dto.setCodePostal(l.getCodePostal());
        dto.setPays(l.getPays());
        dto.setTelephoneBureau(l.getTelephoneBureau());
        dto.setFax(l.getFax());
        dto.setConsultationFee(l.getConsultationFee());
        dto.setAscenseur(l.isAscenseur());
        dto.setEntreeAccessible(l.isEntreeAccessible());
        dto.setEtage(l.getEtage());
        dto.setParking(l.getParking());
        dto.setContactUrgenceType(l.getContactUrgenceType());
        dto.setTelephoneUrgence(l.getTelephoneUrgence());
        dto.setActif(l.isActif());
        if (l.getHoraires() != null) {
            dto.setHoraires(l.getHoraires().stream().map(HoraireDTO::fromEntity).toList());
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

    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

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
        private Long id;
        private JourSemaine jour;
        private LocalTime heureDebut;
        private LocalTime heureFin;
        private boolean continu;

        public static HoraireDTO fromEntity(HoraireOuverture h) {
            HoraireDTO dto = new HoraireDTO();
            dto.setId(h.getId());
            dto.setJour(h.getJour());
            dto.setHeureDebut(h.getHeureDebut());
            dto.setHeureFin(h.getHeureFin());
            dto.setContinu(h.isContinu());
            return dto;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public JourSemaine getJour() { return jour; }
        public void setJour(JourSemaine jour) { this.jour = jour; }

        public LocalTime getHeureDebut() { return heureDebut; }
        public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

        public LocalTime getHeureFin() { return heureFin; }
        public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

        public boolean isContinu() { return continu; }
        public void setContinu(boolean continu) { this.continu = continu; }
    }
}
