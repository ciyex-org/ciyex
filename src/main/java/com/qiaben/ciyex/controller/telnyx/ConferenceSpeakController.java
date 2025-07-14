package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceSpeakRequestDto;
import com.qiaben.ciyex.dto.telnyx.ConferenceSpeakResponseDto;
import com.qiaben.ciyex.service.telnyx.ConferenceSpeakService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceSpeakController {

    private final ConferenceSpeakService speakService;

    @PostMapping("/{conferenceId}/actions/speak")
    public ConferenceSpeakResponseDto speakToParticipants(
            @PathVariable String conferenceId,
            @RequestBody ConferenceSpeakRequestDto request) {
        return speakService.speak(conferenceId, request);
    }
}
