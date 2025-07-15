package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateVerificationRequestDTO;
import com.qiaben.ciyex.dto.telnyx.UpdateVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.UpdateVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class UpdateVerificationRequestController {

    private final UpdateVerificationRequestService service;

    @PatchMapping("/{id}")
    public UpdateVerificationResponseDTO updateRequest(
            @PathVariable String id,
            @RequestBody UpdateVerificationRequestDTO dto
    ) {
        return service.updateRequest(id, dto);
    }
}
