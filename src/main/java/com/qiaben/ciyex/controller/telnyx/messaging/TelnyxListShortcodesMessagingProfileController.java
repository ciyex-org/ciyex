package com.qiaben.ciyex.controller.telnyx.messaging;

import com.qiaben.ciyex.dto.telnyx.messaging.TelnyxListShortcodesMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.messaging.TelnyxListShortcodesMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class TelnyxListShortcodesMessagingProfileController {

    private final TelnyxListShortcodesMessagingProfileService telnyxListShortcodesMessagingProfileService;

    @Autowired
    public TelnyxListShortcodesMessagingProfileController(TelnyxListShortcodesMessagingProfileService telnyxListShortcodesMessagingProfileService) {
        this.telnyxListShortcodesMessagingProfileService = telnyxListShortcodesMessagingProfileService;
    }

    @GetMapping("/messaging_profiles/{id}/short_codes")
    public TelnyxListShortcodesMessagingProfileDto getShortcodes(@PathVariable("id") String id,
                                                                 @RequestParam(defaultValue = "1") int pageNumber,
                                                                 @RequestParam(defaultValue = "20") int pageSize) {
        return telnyxListShortcodesMessagingProfileService.getShortcodes(id, pageNumber, pageSize);
    }
}
