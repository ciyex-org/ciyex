package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SubmitVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.SubmitVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.SubmitVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class SubmitVerificationRequestController {

    private final SubmitVerificationRequestService service;

    @PostMapping
    public SubmitVerificationResponseDTO submitVerificationRequest(
            @RequestBody SubmitVerificationRequestDTO dto
    ) {
        return service.submitRequest(dto);
    }
}
