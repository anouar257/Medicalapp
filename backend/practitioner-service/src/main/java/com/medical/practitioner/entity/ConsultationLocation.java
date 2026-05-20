package com.medical.practitioner.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

/**
 * Lieu de consultation rattaché à un praticien.
 *
 * <p>Cf. cahier des charges : « Créer un lieu ou + de consultation : adresse, nom de
 * l'établissement, téléphone du bureau, fax, accessibilité (cocher ascenseur, entrée
 * accessible), étage, parking (aucun, gratuit, payant), horaire d'ouverture pour chaque
 * jour (matin et après-midi / continu), contact d'urgence, téléphone d'urgence ».
 */
@Entity
@Table(name = "consultation_locations")
public class ConsultationLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "practitioner_id", nullable = false)
    private PractitionerProfile practitioner;

    @NotBlank
    @Column(name = "nom_etablissement", nullable = false, length = 200)
    private String nomEtablissement;

    @NotBlank
    @Column(nullable = false, length = 500)
    private String adresse;

    @Column(length = 120)
    private String ville;

    @Column(name = "code_postal", length = 20)
    private String codePostal;

    @Column(length = 100)
    private String pays = "France";

    @Column(name = "telephone_bureau", length = 30)
    private String telephoneBureau;

    @Column(length = 30)
    private String fax;

    /** Accessibilité — ascenseur disponible. */
    @Column(nullable = false)
    private boolean ascenseur = false;

    /** Accessibilité — entrée accessible (PMR). */
    @Column(name = "entree_accessible", nullable = false)
    private boolean entreeAccessible = false;

    @Column(length = 20)
    private String etage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ParkingType parking = ParkingType.AUCUN;

    /** Type de contact d'urgence (cf. cahier : SECRETARIAT / SOS_MEDECINS / NUMERO_PERSONNEL / NUMERO_DIRECT). */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_urgence_type", length = 30)
    private ContactUrgenceType contactUrgenceType;

    @Column(name = "telephone_urgence", length = 30)
    private String telephoneUrgence;

    /** Lieu actif (peut être archivé sans suppression). */
    @Column(nullable = false)
    private boolean actif = true;

    @OneToMany(mappedBy = "lieu", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HoraireOuverture> horaires = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PractitionerProfile getPractitioner() { return practitioner; }
    public void setPractitioner(PractitionerProfile practitioner) { this.practitioner = practitioner; }

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

    public List<HoraireOuverture> getHoraires() { return horaires; }
    public void setHoraires(List<HoraireOuverture> horaires) { this.horaires = horaires; }
}
