package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CredentialsDto;
import com.qiaben.ciyex.dto.telnyx.CredentialsListResponseDto;
import com.qiaben.ciyex.service.telnyx.CredentialsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/credentials")
@RequiredArgsConstructor
public class CredentialsController {

    private final CredentialsService service;

    @GetMapping
    public CredentialsListResponseDto list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "resource_id") String resourceId,
            @RequestParam(required = false, name = "sip_username") String sipUsername
    ) {
        return service.list(page, size, tag, name, status, resourceId, sipUsername);
    }

    @PostMapping
    public CredentialsDto create(@RequestBody CredentialsDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public CredentialsDto update(@PathVariable String id, @RequestBody CredentialsDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}")
    public CredentialsDto getById(@PathVariable String id) {
        return service.getById(id);
    }
}
