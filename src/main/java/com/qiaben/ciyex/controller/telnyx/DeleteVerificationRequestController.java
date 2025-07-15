package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.service.telnyx.DeleteVerificationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/verification-requests")
@RequiredArgsConstructor
public class DeleteVerificationRequestController {

    private final DeleteVerificationRequestService service;

    @DeleteMapping("/{id}")
    public String deleteVerificationRequest(@PathVariable String id) {
        return service.deleteVerificationRequest(id);
    }
}
