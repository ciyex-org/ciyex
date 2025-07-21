package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxDeleteMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxDeleteMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxDeleteMessagingProfileController {

    private final TelnyxDeleteMessagingProfileService telnyxDeleteMessagingProfileService;

    @Autowired
    public TelnyxDeleteMessagingProfileController(TelnyxDeleteMessagingProfileService telnyxDeleteMessagingProfileService) {
        this.telnyxDeleteMessagingProfileService = telnyxDeleteMessagingProfileService;
    }

    @DeleteMapping("/messaging_profiles/{id}")
    public TelnyxDeleteMessagingProfileDto deleteMessagingProfile(@PathVariable("id") String id) {
        return telnyxDeleteMessagingProfileService.deleteMessagingProfile(id);
    }
}
