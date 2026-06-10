package com.medical.practitioner.controller;

import com.medical.practitioner.dto.PractitionerProfileDTO;
import com.medical.practitioner.entity.ProUser;
import com.medical.practitioner.service.PractitionerPhotoService;
import com.medical.practitioner.service.PractitionerService;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
public class PractitionerPhotoController {

    private final PractitionerService practitionerService;
    private final PractitionerPhotoService practitionerPhotoService;

    public PractitionerPhotoController(
            PractitionerService practitionerService,
            PractitionerPhotoService practitionerPhotoService) {
        this.practitionerService = practitionerService;
        this.practitionerPhotoService = practitionerPhotoService;
    }

    /**
     * Upload photo de profil du praticien connecté.
     */
    @PostMapping(path = "/api/pro/practitioners/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PractitionerProfileDTO uploadPhoto(
            @AuthenticationPrincipal ProUser user,
            @RequestPart("file") MultipartFile file) {
        return practitionerService.savePhoto(user.getId(), file);
    }

    /**
     * Téléchargement public de la photo.
     */
    @GetMapping("/api/pro/public/practitioners/photos/{filename:.+}")
    public ResponseEntity<Resource> downloadPhoto(@PathVariable String filename) {
        Resource resource = practitionerPhotoService.loadAsResource(filename);
        MediaType ct = practitionerPhotoService.probeMediaType(filename);
        return ResponseEntity.ok()
                .contentType(ct)
                .cacheControl(CacheControl.noCache())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
