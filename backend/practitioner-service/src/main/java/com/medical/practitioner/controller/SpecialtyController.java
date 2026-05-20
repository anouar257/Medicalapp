package com.medical.practitioner.controller;

import com.medical.practitioner.dto.SpecialtyDTO;
import com.medical.practitioner.service.SpecialtyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catalogue des spécialités. La liste publique est lue par la page d'inscription pro
 * sans token JWT (cf. {@code /public/list}).
 */
@RestController
@RequestMapping("/api/pro/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping("/public/list")
    public List<SpecialtyDTO> listPublic() {
        return specialtyService.findAll();
    }

    @GetMapping
    public List<SpecialtyDTO> list() {
        return specialtyService.findAll();
    }
}
