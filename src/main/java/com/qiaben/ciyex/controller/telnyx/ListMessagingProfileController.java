package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ListMessagingProfileDto;
import com.qiaben.ciyex.service.telnyx.ListMessagingProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/messaging-profiles")
@RequiredArgsConstructor
public class ListMessagingProfileController {

    private final ListMessagingProfileService service;

    @GetMapping
    public ListMessagingProfileDto getMessagingProfiles(
            @RequestParam(value = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(value = "filterName", required = false) String filterName
    ) {
        return service.listMessagingProfiles(pageNumber, pageSize, filterName);
    }
}
