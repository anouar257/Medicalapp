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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Profil détaillé du praticien (personne physique).
 *
 * <p>Cf. cahier des charges :
 * <ul>
 *   <li>Civilité (Mme/M.), Titre (Aucun/Dr./Pr.) ;</li>
 *   <li>Prénom, Nom, Sexe, Date de naissance ;</li>
 *   <li>Statut (Remplaçant, Collaborateur, Assistant, Interne, Associé, Indisponible) ;</li>
 *   <li>Vérifications : identité (passeport/CIN), droit d'exercer (document ministère) ;</li>
 *   <li>Spécialités, diplômes, certifications, liens (YouTube, etc.) ;</li>
 *   <li>Empreinte (identifiant unique).</li>
 * </ul>
 */
@Entity
@Table(name = "practitioner_profiles")
public class PractitionerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_user_id", nullable = false, unique = true)
    private ProUser proUser;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Civilite civilite;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Titre titre = Titre.AUCUN;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sexe sexe;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private StatutPraticien statut = StatutPraticien.TITULAIRE;

    /** Empreinte unique (identifiant de facturation/sécurité). Cf. cahier : « Empreinte ». */
    @Column(unique = true, length = 100)
    private String empreinte;

    /** Lien public (YouTube, site personnel, etc.). */
    @Column(name = "lien_youtube", length = 500)
    private String lienYoutube;

    @Column(name = "site_web", length = 500)
    private String siteWeb;

    @Column(length = 2000)
    private String biographie;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    /** Couleur affichée dans l'agenda (cohérente avec la Doctor entity de l'agenda-service). */
    @Column(name = "color_code", length = 16)
    private String colorCode = "#0ea5e9";

    // ── Vérifications (cahier : VÉRIFICATION DU COMPTE) ─────────────────────

    /** Statut de la vérification d'identité (passeport / carte d'identité). */
    @Enumerated(EnumType.STRING)
    @Column(name = "verif_identite_status", nullable = false, length = 20)
    private VerificationStatus verifIdentiteStatus = VerificationStatus.NON_FOURNI;

    /** URL du document d'identité fourni (stockage local au service). */
    @Column(name = "doc_identite_url", length = 512)
    private String docIdentiteUrl;

    /** Statut de la vérification du droit d'exercer (document du ministère). */
    @Enumerated(EnumType.STRING)
    @Column(name = "verif_droit_exercer_status", nullable = false, length = 20)
    private VerificationStatus verifDroitExercerStatus = VerificationStatus.NON_FOURNI;

    /** URL du document ministériel fourni (autorisation d'exercer). */
    @Column(name = "doc_droit_exercer_url", length = 512)
    private String docDroitExercerUrl;

    /** Profil disponible / indisponible — cohérent avec {@link StatutPraticien#INDISPONIBLE}. */
    @Column(name = "disponible", nullable = false)
    private boolean disponible = true;

    // ── Relations ──────────────────────────────────────────────────────────

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "practitioner_specialties",
        joinColumns = @JoinColumn(name = "practitioner_id"),
        inverseJoinColumns = @JoinColumn(name = "specialty_id")
    )
    private Set<Specialty> specialites = new HashSet<>();

    @OneToMany(mappedBy = "practitioner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Diploma> diplomes = new ArrayList<>();

    @OneToMany(mappedBy = "practitioner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConsultationLocation> lieuxConsultation = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProUser getProUser() { return proUser; }
    public void setProUser(ProUser proUser) { this.proUser = proUser; }

    public Civilite getCivilite() { return civilite; }
    public void setCivilite(Civilite civilite) { this.civilite = civilite; }

    public Titre getTitre() { return titre; }
    public void setTitre(Titre titre) { this.titre = titre; }

    public Sexe getSexe() { return sexe; }
    public void setSexe(Sexe sexe) { this.sexe = sexe; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public StatutPraticien getStatut() { return statut; }
    public void setStatut(StatutPraticien statut) { this.statut = statut; }

    public String getEmpreinte() { return empreinte; }
    public void setEmpreinte(String empreinte) { this.empreinte = empreinte; }

    public String getLienYoutube() { return lienYoutube; }
    public void setLienYoutube(String lienYoutube) { this.lienYoutube = lienYoutube; }

    public String getSiteWeb() { return siteWeb; }
    public void setSiteWeb(String siteWeb) { this.siteWeb = siteWeb; }

    public String getBiographie() { return biographie; }
    public void setBiographie(String biographie) { this.biographie = biographie; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getColorCode() { return colorCode; }
    public void setColorCode(String colorCode) { this.colorCode = colorCode; }

    public VerificationStatus getVerifIdentiteStatus() { return verifIdentiteStatus; }
    public void setVerifIdentiteStatus(VerificationStatus verifIdentiteStatus) { this.verifIdentiteStatus = verifIdentiteStatus; }

    public String getDocIdentiteUrl() { return docIdentiteUrl; }
    public void setDocIdentiteUrl(String docIdentiteUrl) { this.docIdentiteUrl = docIdentiteUrl; }

    public VerificationStatus getVerifDroitExercerStatus() { return verifDroitExercerStatus; }
    public void setVerifDroitExercerStatus(VerificationStatus verifDroitExercerStatus) { this.verifDroitExercerStatus = verifDroitExercerStatus; }

    public String getDocDroitExercerUrl() { return docDroitExercerUrl; }
    public void setDocDroitExercerUrl(String docDroitExercerUrl) { this.docDroitExercerUrl = docDroitExercerUrl; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public Set<Specialty> getSpecialites() { return specialites; }
    public void setSpecialites(Set<Specialty> specialites) { this.specialites = specialites; }

    public List<Diploma> getDiplomes() { return diplomes; }
    public void setDiplomes(List<Diploma> diplomes) { this.diplomes = diplomes; }

    public List<ConsultationLocation> getLieuxConsultation() { return lieuxConsultation; }
    public void setLieuxConsultation(List<ConsultationLocation> lieuxConsultation) { this.lieuxConsultation = lieuxConsultation; }
}
