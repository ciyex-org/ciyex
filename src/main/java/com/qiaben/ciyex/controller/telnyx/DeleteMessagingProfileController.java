package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.DeleteMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.DeleteMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class DeleteMessagingProfileController {

    private final DeleteMessagingProfileService deleteMessagingProfileService;

    @Autowired
    public DeleteMessagingProfileController(DeleteMessagingProfileService deleteMessagingProfileService) {
        this.deleteMessagingProfileService = deleteMessagingProfileService;
    }

    @DeleteMapping("/messaging_profiles/{id}")
    public DeleteMessagingProfileDto deleteMessagingProfile(@PathVariable("id") String id) {
        return deleteMessagingProfileService.deleteMessagingProfile(id);
    }
}
