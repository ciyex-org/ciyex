package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialConnectionListResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialConnectionRegistrationStatusDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialConnectionResponseDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCredentialConnectionUpdateDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCredentialConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/credential-connections")
@RequiredArgsConstructor
public class TelnyxCredentialConnectionController {

    private final TelnyxCredentialConnectionService service;

    @GetMapping("/{id}")
    public TelnyxCredentialConnectionResponseDto getCredentialConnectionById(@PathVariable String id) {
        return service.getCredentialConnectionById(id);
    }

    @GetMapping
    public TelnyxCredentialConnectionListResponseDto listCredentialConnections() {
        return service.listCredentialConnections();
    }

    @PatchMapping("/{id}")
    public TelnyxCredentialConnectionResponseDto updateCredentialConnection(@PathVariable String id,
                                                                            @RequestBody TelnyxCredentialConnectionUpdateDto dto) {
        return service.updateCredentialConnection(id, dto);
    }

    @DeleteMapping("/{id}")
    public void deleteCredentialConnection(@PathVariable String id) {
        service.deleteCredentialConnection(id);
    }

    @PostMapping("/{id}/actions/check_registration_status")
    public TelnyxCredentialConnectionRegistrationStatusDto checkRegistrationStatus(@PathVariable String id) {
        return service.checkRegistrationStatus(id);
    }
}
