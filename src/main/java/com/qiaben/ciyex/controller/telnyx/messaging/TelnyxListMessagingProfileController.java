package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxListMessagingProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-profiles")
@RequiredArgsConstructor
public class TelnyxListMessagingProfileController {

    private final TelnyxListMessagingProfileService service;

    @GetMapping
    public TelnyxListMessagingProfileDto getMessagingProfiles(
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(value = "filterName", required = false) String filterName
    ) {
        return service.listMessagingProfiles(pageNumber, pageSize, filterName);
    }
}
