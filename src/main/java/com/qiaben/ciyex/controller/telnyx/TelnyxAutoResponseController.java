package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxAutoResponseDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxAutoResponseListResponse;
import com.qiaben.ciyex.dto.telnyx.TelnyxAutoResponseRequest;
import com.qiaben.ciyex.service.telnyx.TelnyxAutoResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/voice/auto-response")
@RequiredArgsConstructor
public class TelnyxAutoResponseController {

    private final TelnyxAutoResponseService service;

    @GetMapping("/{profileId}")
    public ResponseEntity<TelnyxAutoResponseListResponse> listConfigs(
            @PathVariable String profileId,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false, name = "created_at[gte]") String createdAfter,
            @RequestParam(required = false, name = "created_at[lte]") String createdBefore,
            @RequestParam(required = false, name = "updated_at[gte]") String updatedAfter,
            @RequestParam(required = false, name = "updated_at[lte]") String updatedBefore
    ) {
        return ResponseEntity.ok(service.list(profileId, countryCode, createdAfter, createdBefore, updatedAfter, updatedBefore));
    }

    @PostMapping("/{profileId}")
    public ResponseEntity<TelnyxAutoResponseDTO> createConfig(
            @PathVariable String profileId,
            @RequestBody TelnyxAutoResponseRequest request
    ) {
        return ResponseEntity.ok(service.create(profileId, request));
    }

    @GetMapping("/{profileId}/{configId}")
    public ResponseEntity<TelnyxAutoResponseDTO> getConfig(
            @PathVariable String profileId,
            @PathVariable String configId
    ) {
        return ResponseEntity.ok(service.getById(profileId, configId));
    }

    @PatchMapping("/{profileId}/{configId}")
    public ResponseEntity<TelnyxAutoResponseDTO> updateConfig(
            @PathVariable String profileId,
            @PathVariable String configId,
            @RequestBody TelnyxAutoResponseRequest request
    ) {
        return ResponseEntity.ok(service.update(profileId, configId, request));
    }

    @DeleteMapping("/{profileId}/{configId}")
    public ResponseEntity<Void> deleteConfig(
            @PathVariable String profileId,
            @PathVariable String configId
    ) {
        service.delete(profileId, configId);
        return ResponseEntity.noContent().build();
    }
}
