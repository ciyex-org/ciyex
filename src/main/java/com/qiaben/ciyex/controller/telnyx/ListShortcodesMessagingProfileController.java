package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListShortcodesMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.ListShortcodesMessagingProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telynx")
public class ListShortcodesMessagingProfileController {

    private final ListShortcodesMessagingProfileService listShortcodesMessagingProfileService;

    @Autowired
    public ListShortcodesMessagingProfileController(ListShortcodesMessagingProfileService listShortcodesMessagingProfileService) {
        this.listShortcodesMessagingProfileService = listShortcodesMessagingProfileService;
    }

    @GetMapping("/messaging_profiles/{id}/short_codes")
    public ListShortcodesMessagingProfileDto getShortcodes(@PathVariable("id") String id,
                                                           @RequestParam(defaultValue = "1") int pageNumber,
                                                           @RequestParam(defaultValue = "20") int pageSize) {
        return listShortcodesMessagingProfileService.getShortcodes(id, pageNumber, pageSize);
    }
}
