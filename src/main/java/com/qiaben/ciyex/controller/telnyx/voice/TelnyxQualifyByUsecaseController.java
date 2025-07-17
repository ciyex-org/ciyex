package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxQualifyByUsecaseResponseDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxQualifyByUsecaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/campaign")
@RequiredArgsConstructor
public class TelnyxQualifyByUsecaseController {

    private final TelnyxQualifyByUsecaseService service;

    @GetMapping("/qualify/{brandId}/{usecase}")
    public ResponseEntity<TelnyxQualifyByUsecaseResponseDTO> qualify(
            @PathVariable String brandId,
            @PathVariable String usecase) {
        return ResponseEntity.ok(service.qualify(brandId, usecase));
    }
}
