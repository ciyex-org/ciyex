package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.JoinConferenceRequestDto;
import com.qiaben.ciyex.dto.telnyx.JoinConferenceResponseDto;
import com.qiaben.ciyex.service.telnyx.JoinConferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class JoinConferenceController {

    private final JoinConferenceService joinConferenceService;

    @PostMapping("/{conferenceId}/actions/join")
    public JoinConferenceResponseDto joinConference(
            @PathVariable String conferenceId,
            @RequestBody JoinConferenceRequestDto request) {
        return joinConferenceService.joinConference(conferenceId, request);
    }
}
