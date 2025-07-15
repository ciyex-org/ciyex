package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RetrieveMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.RetrieveMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class RetrieveMessagingProfileController {

    private final RetrieveMessagingProfileService retrieveMessagingProfileService;

    @Autowired
    public RetrieveMessagingProfileController(RetrieveMessagingProfileService retrieveMessagingProfileService) {
        this.retrieveMessagingProfileService = retrieveMessagingProfileService;
    }

    @GetMapping("/messaging_profiles/{id}")
    public RetrieveMessagingProfileDto getMessagingProfile(@PathVariable("id") String id) {
        return retrieveMessagingProfileService.getMessagingProfile(id);
    }
}
