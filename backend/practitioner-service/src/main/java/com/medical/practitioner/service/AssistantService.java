package com.medical.practitioner.service;

import com.medical.practitioner.dto.CreateAssistantRequest;
import com.medical.practitioner.dto.CreateProUserRequest;
import com.medical.practitioner.dto.ProUserDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.entity.ProUserRole;
import com.medical.practitioner.repository.ProUserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssistantService {

    private final ProUserRepository proUserRepository;
    private final CabinetService cabinetService;

    public AssistantService(ProUserRepository proUserRepository, CabinetService cabinetService) {
        this.proUserRepository = proUserRepository;
        this.cabinetService = cabinetService;
    }

    /**
     * Téléphone technique unique (le formulaire cabinet ne collecte pas le mobile de l'assistant).
     * Contrainte {@code pro_users.telephone} : unique, max 30 caractères.
     */
    private static String syntheticAssistantTelephone() {
        String u = UUID.randomUUID().toString().replace("-", "");
        String raw = "ASST" + u;
        return raw.length() <= 30 ? raw : raw.substring(0, 30);
    }

    @Transactional
    public ProUserDTO createAssistant(Long creatorProUserId, CreateAssistantRequest req) {
        ProUser creator =
                proUserRepository
                        .findByIdWithOrganization(creatorProUserId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur introuvable"));
        if (creator.getOrganization() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun cabinet associé à ce compte.");
        }
        Long organizationId = creator.getOrganization().getId();

        CreateProUserRequest inner = new CreateProUserRequest();
        inner.setEmail(req.getEmail());
        inner.setTelephone(syntheticAssistantTelephone());
        inner.setPrenom(req.getPrenom());
        inner.setNom(req.getNom());
        inner.setRole(ProUserRole.ASSISTANT);
        inner.setMotDePasse(req.getMotDePasse());

        return cabinetService.createUserInCabinet(creator, organizationId, inner);
    }

    @Transactional(readOnly = true)
    public List<ProUserDTO> listAssistantsInMyCabinet(Long proUserId) {
        ProUser user =
                proUserRepository
                        .findByIdWithOrganization(proUserId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur introuvable"));
        if (user.getOrganization() == null) {
            return List.of();
        }
        return proUserRepository
                .findByOrganizationIdAndRole(user.getOrganization().getId(), ProUserRole.ASSISTANT)
                .stream()
                .map(ProUserDTO::fromEntity)
                .toList();
    }
}
