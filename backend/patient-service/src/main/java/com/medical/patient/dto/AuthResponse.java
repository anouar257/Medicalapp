package com.medical.patient.dto;

import com.medical.patient.entity.Sexe;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse d'authentification — token JWT + informations du patient.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private Long patientId;
    private String email;
    private String prenom;
    private String nom;
    private Sexe sexe;
    private LocalDate dateNaissance;
    private String telephone;
    private boolean emailVerifie;
    private boolean telephoneVerifie;
    private String ville;

}
