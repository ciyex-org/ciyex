package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxUpdateMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxUpdateMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxUpdateMessagingProfileController {

    private final TelnyxUpdateMessagingProfileService telnyxUpdateMessagingProfileService;

    @Autowired
    public TelnyxUpdateMessagingProfileController(TelnyxUpdateMessagingProfileService telnyxUpdateMessagingProfileService) {
        this.telnyxUpdateMessagingProfileService = telnyxUpdateMessagingProfileService;
    }

    @PatchMapping("/messaging_profiles/{id}")
    public TelnyxUpdateMessagingProfileDto updateMessagingProfile(
            @PathVariable("id") String id,
            @RequestBody TelnyxUpdateMessagingProfileDto updateMessagingProfileDto) {
        return telnyxUpdateMessagingProfileService.updateMessagingProfile(id, updateMessagingProfileDto);
    }
}
