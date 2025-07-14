package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.CallControlApplicationDTO;
import com.qiaben.ciyex.service.telnyx.CallControlApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telnyx/call-control-applications")
@RequiredArgsConstructor
public class CallControlApplicationController {

    private final CallControlApplicationService svc;

    /* LIST */
    @GetMapping
    public Object list(@RequestParam Map<String, String> params) {
        return svc.list(params);
    }

    /* CREATE */
    @PostMapping
    public Object create(@RequestBody CallControlApplicationDTO dto) {
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
            @RequestBody CallControlApplicationDTO dto) {
        return svc.update(id, dto);
    }

    /* DELETE */
    @DeleteMapping("/{id}")
    public Object delete(@PathVariable Long id) {
        return svc.delete(id);
    }
}
