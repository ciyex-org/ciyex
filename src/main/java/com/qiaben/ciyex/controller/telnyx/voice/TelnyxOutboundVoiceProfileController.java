package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxOutboundVoiceProfileDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxOutboundVoiceProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/telnyx/voice-profiles")
@RequiredArgsConstructor
public class TelnyxOutboundVoiceProfileController {

    private final TelnyxOutboundVoiceProfileService service;

    @GetMapping("/{id}")
    public TelnyxOutboundVoiceProfileDTO getById(@PathVariable Long id) {
        return service.retrieveById(id);
    }

    @GetMapping
    public List<TelnyxOutboundVoiceProfileDTO> getAll() {
        return service.listAll();
    }

    @PostMapping
    public TelnyxOutboundVoiceProfileDTO create(@RequestBody TelnyxOutboundVoiceProfileDTO dto) {
        return service.create(dto);
    }

    @PatchMapping("/{id}")
    public TelnyxOutboundVoiceProfileDTO update(@PathVariable Long id, @RequestBody TelnyxOutboundVoiceProfileDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
