package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.ConferenceParticipantListDto;
import com.qiaben.ciyex.service.telnyx.ConferenceParticipantListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class ConferenceParticipantListController {

    private final ConferenceParticipantListService participantListService;

    @GetMapping("/{conferenceId}/participants")
    public ConferenceParticipantListDto listParticipants(
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
