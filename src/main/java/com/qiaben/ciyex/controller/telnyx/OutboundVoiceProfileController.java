package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.OutboundVoiceProfileDTO;
import com.qiaben.ciyex.service.telnyx.OutboundVoiceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telnyx/voice-profiles")
@RequiredArgsConstructor
public class OutboundVoiceProfileController {

    private final OutboundVoiceProfileService service;

    @GetMapping("/{id}")
    public OutboundVoiceProfileDTO getById(@PathVariable Long id) {
        return service.retrieveById(id);
    }

    @GetMapping
    public List<OutboundVoiceProfileDTO> getAll() {
        return service.listAll();
    }

    @PostMapping
    public OutboundVoiceProfileDTO create(@RequestBody OutboundVoiceProfileDTO dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public OutboundVoiceProfileDTO update(@PathVariable Long id, @RequestBody OutboundVoiceProfileDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
