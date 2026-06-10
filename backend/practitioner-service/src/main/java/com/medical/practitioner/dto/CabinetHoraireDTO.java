package com.medical.practitioner.dto;

import com.medical.practitioner.entity.CabinetHoraire;
import com.medical.practitioner.entity.JourSemaine;
import java.time.LocalTime;

public class CabinetHoraireDTO {
    private Long id;
    private JourSemaine jour;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    private boolean continu;

    public static CabinetHoraireDTO fromEntity(CabinetHoraire h) {
        CabinetHoraireDTO dto = new CabinetHoraireDTO();
        dto.id = h.getId();
        dto.jour = h.getJour();
        dto.heureDebut = h.getHeureDebut();
        dto.heureFin = h.getHeureFin();
        dto.continu = h.isContinu();
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
