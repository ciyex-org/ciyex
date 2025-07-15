package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceCreateController {

    private final ConferenceCreateService conferenceService;

    /**
     * Proxy endpoint that simply forwards to Telnyx.
     */
    @PostMapping
    public ConferenceResponseDto createConference(
            @RequestBody ConferenceCreateRequestDto body) {

        return conferenceService.createConference(body);
    }
}
