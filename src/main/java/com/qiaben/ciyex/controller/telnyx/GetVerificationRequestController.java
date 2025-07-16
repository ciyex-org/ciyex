package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.GetVerificationResponseDTO;
import com.qiaben.ciyex.service.telnyx.GetVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class GetVerificationRequestController {

    private final GetVerificationRequestService service;

    @GetMapping("/{id}")
    public GetVerificationResponseDTO getVerificationById(@PathVariable String id) {
        return service.getVerificationRequestById(id);
    }
}
