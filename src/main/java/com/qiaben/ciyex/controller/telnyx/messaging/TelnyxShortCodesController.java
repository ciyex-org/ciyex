package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxShortCodesDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxSingleShortCodeDto;
import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateShortCodeRequest;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxShortCodesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx/short_codes")
public class TelnyxShortCodesController {

    private final TelnyxShortCodesService telnyxShortCodesService;

    @Autowired
    public TelnyxShortCodesController(TelnyxShortCodesService telnyxShortCodesService) {
        this.telnyxShortCodesService = telnyxShortCodesService;
    }

    // List
    @GetMapping
    public TelnyxShortCodesDto listShortCodes(
            @RequestParam(value = "page[number]", required = false) Integer pageNumber,
            @RequestParam(value = "page[size]", required = false) Integer pageSize,
            @RequestParam(value = "filter[messaging_profile_id]", required = false) String messagingProfileId
    ) {
        return telnyxShortCodesService.listShortCodes(pageNumber, pageSize, messagingProfileId);
    }

    // Retrieve single
    @GetMapping("/{id}")
    public TelnyxSingleShortCodeDto getShortCode(@PathVariable String id) {
        return telnyxShortCodesService.getShortCode(id);
    }

    // Update
    @PatchMapping("/{id}")
    public TelnyxSingleShortCodeDto updateShortCode(@PathVariable String id, @RequestBody TelnyxUpdateShortCodeRequest request) {
        return telnyxShortCodesService.updateShortCode(id, request);
    }
}
