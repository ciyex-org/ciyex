package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.service.telnyx.messaging.TelnyxResendBrand2FAResponseDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxResendBrand2FAService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/brand")
@RequiredArgsConstructor
public class TelnyxResendBrand2FAController {

    private final TelnyxResendBrand2FAService service;

    @PostMapping("/{brandId}/resend-2fa-email")
    public ResponseEntity<TelnyxResendBrand2FAResponseDto> resend2FA(@PathVariable String brandId) {
        TelnyxResendBrand2FAResponseDto response = service.resend2FAEmail(brandId);
        return ResponseEntity.ok(response);
    }
}