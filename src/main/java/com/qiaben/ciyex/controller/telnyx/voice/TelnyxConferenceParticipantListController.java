package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxConferenceParticipantListDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxConferenceParticipantListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class TelnyxConferenceParticipantListController {

    private final TelnyxConferenceParticipantListService participantListService;

    @GetMapping("/{conferenceId}/participants")
    public TelnyxConferenceParticipantListDto listParticipants(
            @PathVariable String conferenceId,
            @RequestParam(value = "filter[muted]", required = false) Boolean muted,
            @RequestParam(value = "filter[on_hold]", required = false) Boolean onHold,
            @RequestParam(value = "filter[whispering]", required = false) Boolean whispering,
            @RequestParam(value = "page[number]", required = false) Integer pageNumber,
            @RequestParam(value = "page[size]", required = false) Integer pageSize) {

        return participantListService.listParticipants(
                conferenceId, muted, onHold, whispering, pageNumber, pageSize);
    }
}
