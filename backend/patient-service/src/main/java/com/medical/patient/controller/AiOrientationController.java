package com.medical.patient.controller;

import com.medical.patient.dto.AiOrientationRequest;
import com.medical.patient.dto.AiOrientationResponse;
import com.medical.patient.service.AiOrientationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public proxy endpoint for the AI Orientation assistant.
 */
@RestController
@RequestMapping("/api/public/ai")
public class AiOrientationController {

    private final AiOrientationService aiOrientationService;

    public AiOrientationController(AiOrientationService aiOrientationService) {
        this.aiOrientationService = aiOrientationService;
    }

    @PostMapping("/orientation")
    public ResponseEntity<AiOrientationResponse> getOrientation(@Valid @RequestBody AiOrientationRequest request) {
        AiOrientationResponse response = aiOrientationService.getOrientation(request);
        return ResponseEntity.ok(response);
    }
}
