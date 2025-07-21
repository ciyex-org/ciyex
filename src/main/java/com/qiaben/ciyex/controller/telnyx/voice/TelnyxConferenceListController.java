package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceListResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceListController {

    private final TelnyxConferenceListService conferenceService;

    @GetMapping
    public TelnyxConferenceListResponseDto listConferences(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return conferenceService.listConferences(name, status, page, size);
    }
}
