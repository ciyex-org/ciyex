package org.ciyex.ehr.payment.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.payment.dto.PaymentConfigDto;
import org.ciyex.ehr.payment.service.PaymentConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasAuthority('SCOPE_user/Organization.read')")
@RestController
@RequestMapping("/api/payments/config")
@RequiredArgsConstructor
@Slf4j
public class PaymentConfigController {

    private final PaymentConfigService service;

    @GetMapping
    public ResponseEntity<ApiResponse<PaymentConfigDto>> getConfig() {
        try {
            var config = service.getConfig();
            return ResponseEntity.ok(ApiResponse.ok("Payment config retrieved", config));
        } catch (Exception e) {
            log.error("Failed to get payment config", e);
            return ResponseEntity.ok(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SCOPE_user/Organization.write')")
    public ResponseEntity<ApiResponse<PaymentConfigDto>> saveConfig(@RequestBody PaymentConfigDto dto) {
        try {
            var config = service.saveConfig(dto);
            return ResponseEntity.ok(ApiResponse.ok("Payment config saved", config));
        } catch (Exception e) {
            log.error("Failed to save payment config", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
