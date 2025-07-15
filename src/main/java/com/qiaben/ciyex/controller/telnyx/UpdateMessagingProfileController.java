package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.UpdateMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class UpdateMessagingProfileController {

    private final UpdateMessagingProfileService updateMessagingProfileService;

    @Autowired
    public UpdateMessagingProfileController(UpdateMessagingProfileService updateMessagingProfileService) {
        this.updateMessagingProfileService = updateMessagingProfileService;
    }

    @PatchMapping("/messaging_profiles/{id}")
    public UpdateMessagingProfileDto updateMessagingProfile(
            @PathVariable("id") String id,
            @RequestBody UpdateMessagingProfileDto updateMessagingProfileDto) {
        return updateMessagingProfileService.updateMessagingProfile(id, updateMessagingProfileDto);
    }
}
