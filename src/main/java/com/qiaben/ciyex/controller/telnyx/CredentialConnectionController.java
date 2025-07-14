package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.*;
import com.qiaben.ciyex.service.telnyx.CredentialConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/credential-connections")
@RequiredArgsConstructor
public class CredentialConnectionController {

    private final CredentialConnectionService service;

    @GetMapping("/{id}")
    public CredentialConnectionResponseDto getCredentialConnectionById(@PathVariable String id) {
        return service.getCredentialConnectionById(id);
    }

    @GetMapping
    public CredentialConnectionListResponseDto listCredentialConnections() {
        return service.listCredentialConnections();
    }

    @PatchMapping("/{id}")
    public CredentialConnectionResponseDto updateCredentialConnection(@PathVariable String id,
                                                                      @RequestBody CredentialConnectionUpdateDto dto) {
        return service.updateCredentialConnection(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteCredentialConnection(@PathVariable String id) {
        service.deleteCredentialConnection(id);
    }

    @PostMapping("/{id}/actions/check_registration_status")
    public CredentialConnectionRegistrationStatusDto checkRegistrationStatus(@PathVariable String id) {
        return service.checkRegistrationStatus(id);
    }
}
