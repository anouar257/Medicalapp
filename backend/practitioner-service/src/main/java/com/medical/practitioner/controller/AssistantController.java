package com.medical.practitioner.controller;

import com.medical.practitioner.dto.CreateAssistantRequest;
import com.medical.practitioner.dto.ProUserDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.service.AssistantService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pro/assistants")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PRATICIEN')")
    public ResponseEntity<ProUserDTO> create(
            @AuthenticationPrincipal ProUser creator, @Valid @RequestBody CreateAssistantRequest body) {
        ProUserDTO created = assistantService.createAssistant(creator.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/my-cabinet")
    @PreAuthorize("hasRole('PRATICIEN')")
    public List<ProUserDTO> listMyCabinetAssistants(@AuthenticationPrincipal ProUser user) {
        return assistantService.listAssistantsInMyCabinet(user.getId());
    }
}
