package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.TelnyxDialRequestDTO;
import com.qiaben.ciyex.dto.telnyx.TelnyxDialResponseDTO;
import com.qiaben.ciyex.service.telnyx.TelnyxDialService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/dial")
@RequiredArgsConstructor
public class TelnyxDialController {

    private final TelnyxDialService dialService;

    /**
     * POST /api/telnyx/dial
     */
    @PostMapping
    public ResponseEntity<TelnyxDialResponseDTO> dial(@RequestBody TelnyxDialRequestDTO body) {
        return ResponseEntity.ok(dialService.dial(body));
    }
}
