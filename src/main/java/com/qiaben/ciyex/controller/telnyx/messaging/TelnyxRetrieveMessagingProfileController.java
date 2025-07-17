package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxRetrieveMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxRetrieveMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxRetrieveMessagingProfileController {

    private final TelnyxRetrieveMessagingProfileService telnyxRetrieveMessagingProfileService;

    @Autowired
    public TelnyxRetrieveMessagingProfileController(TelnyxRetrieveMessagingProfileService telnyxRetrieveMessagingProfileService) {
        this.telnyxRetrieveMessagingProfileService = telnyxRetrieveMessagingProfileService;
    }

    @GetMapping("/messaging_profiles/{id}")
    public TelnyxRetrieveMessagingProfileDto getMessagingProfile(@PathVariable("id") String id) {
        return telnyxRetrieveMessagingProfileService.getMessagingProfile(id);
    }
}
