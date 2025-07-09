package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ImmunizationResponseDTO;
import com.qiaben.ciyex.service.OpenEmrFhirImmunizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OpenEmrFhirImmunizationController {

    private final OpenEmrFhirImmunizationService immunizationService;

    @GetMapping("/fhir/Immunization")
    public ResponseEntity<?> getImmunizations(@RequestParam Map<String, String> queryParams) {
        try {
            return ResponseEntity.ok(immunizationService.getImmunizations(queryParams));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error fetching immunizations");
        }
    }
    @GetMapping("/fhir/Immunization/{uuid}")
    public ResponseEntity<?> getImmunization(@PathVariable String uuid) {
        try {
            ImmunizationResponseDTO immunizationDto = immunizationService.getImmunization(uuid);
            return ResponseEntity.ok(immunizationDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Immunization resource not found");
        }
    }
}
