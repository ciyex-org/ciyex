package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxCallControlApplicationDTO;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxCallControlApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/call-control-applications")
@RequiredArgsConstructor
public class TelnyxCallControlApplicationController {

    private final TelnyxCallControlApplicationService svc;

    /* LIST */
    @GetMapping
    public Object list(@RequestParam Map<String, String> params) {
        return svc.list(params);
    }

    /* CREATE */
    @PostMapping
    public Object create(@RequestBody TelnyxCallControlApplicationDTO dto) {
        return svc.create(dto);
    }

    /* RETRIEVE */
    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) {
        return svc.retrieve(id);
    }

    /* UPDATE */
    @PatchMapping("/{id}")
    public Object update(
            @PathVariable Long id,
            @RequestBody TelnyxCallControlApplicationDTO dto) {
        return svc.update(id, dto);
    }

    /* DELETE */
    @DeleteMapping("/{id}")
    public Object delete(@PathVariable Long id) {
        return svc.delete(id);
    }
}
