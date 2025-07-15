package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.QualifyByUsecaseResponseDTO;
import com.qiaben.ciyex.service.telnyx.QualifyByUsecaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class QualifyByUsecaseController {

    private final QualifyByUsecaseService service;

    @GetMapping("/qualify/{brandId}/{usecase}")
    public ResponseEntity<QualifyByUsecaseResponseDTO> qualify(
            @PathVariable String brandId,
            @PathVariable String usecase) {
        return ResponseEntity.ok(service.qualify(brandId, usecase));
    }
}
