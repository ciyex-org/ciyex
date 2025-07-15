package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ResendBrand2FAResponseDto;
import com.qiaben.ciyex.service.telnyx.ResendBrand2FAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/brand")
@RequiredArgsConstructor
public class ResendBrand2FAController {

    private final ResendBrand2FAService service;

    @PostMapping("/{brandId}/resend-2fa-email")
    public ResponseEntity<ResendBrand2FAResponseDto> resend2FA(@PathVariable String brandId) {
        ResendBrand2FAResponseDto response = service.resend2FAEmail(brandId);
        return ResponseEntity.ok(response);
    }
}