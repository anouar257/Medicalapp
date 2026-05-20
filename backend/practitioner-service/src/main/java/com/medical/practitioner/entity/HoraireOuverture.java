package com.medical.practitioner.entity;

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
import jakarta.persistence.Table;

import java.time.LocalTime;

/**
 * Plage horaire d'ouverture pour un jour de la semaine.
 *
 * <p>Cf. cahier des charges : « horaire d'ouverture pour chaque jour
 * (matin et après-midi / continu) ».
 *
 * <p>On modélise une plage par ligne. Pour un jour avec ouverture matin + après-midi,
 * il suffit de créer 2 lignes (ex. 8h–12h et 14h–18h). Pour un jour en continu,
 * 1 seule ligne (8h–18h) suffit.
 */
@Entity
@Table(name = "horaires_ouverture")
public class HoraireOuverture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieu_id", nullable = false)
    private ConsultationLocation lieu;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private JourSemaine jour;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    /** {@code true} si la plage est en mode « continu » (matin + après-midi sans pause). */
    @Column(nullable = false)
    private boolean continu = false;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ConsultationLocation getLieu() { return lieu; }
    public void setLieu(ConsultationLocation lieu) { this.lieu = lieu; }

    public JourSemaine getJour() { return jour; }
    public void setJour(JourSemaine jour) { this.jour = jour; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public LocalTime getHeureFin() { return heureFin; }
    public void setHeureFin(LocalTime heureFin) { this.heureFin = heureFin; }

    public boolean isContinu() { return continu; }
    public void setContinu(boolean continu) { this.continu = continu; }
}
