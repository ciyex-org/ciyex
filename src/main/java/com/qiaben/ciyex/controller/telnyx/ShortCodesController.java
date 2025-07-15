package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ShortCodesDto;
import com.qiaben.ciyex.dto.telnyx.SingleShortCodeDto;
import com.qiaben.ciyex.dto.telnyx.UpdateShortCodeRequest;
import com.qiaben.ciyex.service.telnyx.ShortCodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/short_codes")
public class ShortCodesController {

    private final ShortCodesService shortCodesService;

    @Autowired
    public ShortCodesController(ShortCodesService shortCodesService) {
        this.shortCodesService = shortCodesService;
    }

    // List
    @GetMapping
    public ShortCodesDto listShortCodes(
            @RequestParam(value = "page[number]", required = false) Integer pageNumber,
            @RequestParam(value = "page[size]", required = false) Integer pageSize,
            @RequestParam(value = "filter[messaging_profile_id]", required = false) String messagingProfileId
    ) {
        return shortCodesService.listShortCodes(pageNumber, pageSize, messagingProfileId);
    }

    // Retrieve single
    @GetMapping("/{id}")
    public SingleShortCodeDto getShortCode(@PathVariable String id) {
        return shortCodesService.getShortCode(id);
    }

    // Update
    @PatchMapping("/{id}")
    public SingleShortCodeDto updateShortCode(@PathVariable String id, @RequestBody UpdateShortCodeRequest request) {
        return shortCodesService.updateShortCode(id, request);
    }
}
