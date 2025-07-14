package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceListResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceListController {

    private final ConferenceListService conferenceService;

    @GetMapping
    public ConferenceListResponseDto listConferences(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return conferenceService.listConferences(name, status, page, size);
    }
}
