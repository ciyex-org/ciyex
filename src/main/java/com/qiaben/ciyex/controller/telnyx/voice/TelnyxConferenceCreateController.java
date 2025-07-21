package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceCreateRequestDto;
import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceCreateController {

    private final TelnyxConferenceCreateService conferenceService;

    /**
     * Proxy endpoint that simply forwards to Telnyx.
     */
    @PostMapping
    public TelnyxConferenceResponseDto createConference(
            @RequestBody TelnyxConferenceCreateRequestDto body) {

        return conferenceService.createConference(body);
    }
}
