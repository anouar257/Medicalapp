package com.medical.practitioner.controller;

import com.medical.practitioner.dto.AuthProResponse;
import com.medical.practitioner.dto.CheckExistenceRequest;
import com.medical.practitioner.dto.ForgotPasswordRequest;
import com.medical.practitioner.dto.LoginProRequest;
import com.medical.practitioner.dto.RegisterCabinetRequest;
import com.medical.practitioner.dto.RegisterPractitionerRequest;
import com.medical.practitioner.dto.ResetPasswordRequest;
import com.medical.practitioner.dto.SendOtpRequest;
import com.medical.practitioner.dto.VerifyOtpRequest;
import com.medical.practitioner.entity.PractitionerProfile;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.service.AuthProService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoints publics — inscription, login, OTP, mot de passe oublié.
 *
 * <p>Tous les chemins sont sous {@code /api/pro/auth/**} et exemptés de JWT
 * dans {@link com.medical.practitioner.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/pro/auth")
public class AuthProController {

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ORGANIZATION_ID = "organizationId";
    private static final String KEY_TELEPHONE = "telephone";
    private static final String KEY_ERROR = "error";
    private static final String KEY_EMAIL = "email";

    private final AuthProService authService;

    public AuthProController(AuthProService authService) {
        this.authService = authService;
    }

    @PostMapping("/check-existence")
    public ResponseEntity<Map<String, Boolean>> checkExistence(@RequestBody CheckExistenceRequest body) {
        boolean exists = authService.existsByEmailOrTelephone(body.getEmail(), body.getTelephone());
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Inscription d'un cabinet — crée l'organisme + le compte praticien titulaire.
     */
    @PostMapping("/register-cabinet")
    public ResponseEntity<Map<String, Object>> registerCabinet(@Valid @RequestBody RegisterCabinetRequest req) {
        try {
            ProUser saved = authService.registerCabinet(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    KEY_MESSAGE, "MedicalOrganization inscrit. Vérifiez votre email et votre téléphone.",
                    KEY_USER_ID, saved.getId(),
                    KEY_ORGANIZATION_ID, saved.getOrganization().getId(),
                    KEY_EMAIL, saved.getEmail(),
                    KEY_TELEPHONE, saved.getTelephone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    /**
     * Inscription self-service d'un praticien (ou remplaçant).
     */
    @PostMapping("/register-practitioner")
    public ResponseEntity<Map<String, Object>> registerPractitioner(@Valid @RequestBody RegisterPractitionerRequest req) {
        try {
            PractitionerProfile saved = authService.registerPractitioner(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    KEY_MESSAGE, "Praticien inscrit. Vérifiez votre email et votre téléphone.",
                    "practitionerProfileId", saved.getId(),
                    KEY_USER_ID, saved.getProUser().getId(),
                    KEY_ORGANIZATION_ID, saved.getProUser().getOrganization().getId(),
                    KEY_EMAIL, saved.getProUser().getEmail(),
                    KEY_TELEPHONE, saved.getProUser().getTelephone()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    /**
     * Login — retourne le rôle exact dans le JWT (pas de choix de rôle côté client).
     */
    @PostMapping("/login")
    public ResponseEntity<Object> login(@Valid @RequestBody LoginProRequest req) {
        try {
            AuthProResponse response = authService.login(req);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        try {
            authService.resendOtp(req);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Code OTP envoyé"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Erreur OTP : " + e.getMessage()));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VerifyOtpRequest req) {
        boolean verified = authService.verifyOtp(req);
        if (verified) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put(KEY_MESSAGE, "Vérification réussie");
            body.put("verified", true);
            authService
                    .tryRefreshJwtAfterSuccessfulOtp(authorization, req)
                    .ifPresent(
                            ar -> {
                                body.put("token", ar.getToken());
                                body.put(KEY_USER_ID, ar.getUserId());
                                body.put(KEY_EMAIL, ar.getEmail());
                                body.put(KEY_TELEPHONE, ar.getTelephone());
                                body.put("prenom", ar.getPrenom());
                                body.put("nom", ar.getNom());
                                body.put("role", ar.getRole());
                                body.put(KEY_ORGANIZATION_ID, ar.getOrganizationId());
                                body.put("organizationNom", ar.getOrganizationNom());
                                body.put("emailVerifie", ar.isEmailVerifie());
                                body.put("telephoneVerifie", ar.isTelephoneVerifie());
                                if (ar.getPractitionerProfileId() != null) {
                                    body.put("practitionerProfileId", ar.getPractitionerProfileId());
                                }
                            });
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, "Code OTP invalide ou expiré", "verified", false));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        try {
            authService.forgotPassword(req);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Instructions de réinitialisation envoyées"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        try {
            authService.resetPassword(req);
            return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Mot de passe réinitialisé avec succès"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(KEY_ERROR, e.getMessage()));
        }
    }
}
