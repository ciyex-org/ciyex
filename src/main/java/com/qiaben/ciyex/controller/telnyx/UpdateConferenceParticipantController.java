package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.UpdateConferenceParticipantRequestDto;
import com.qiaben.ciyex.dto.telnyx.UpdateConferenceParticipantResponseDto;
import com.qiaben.ciyex.service.telnyx.UpdateConferenceParticipantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/conferences")
@RequiredArgsConstructor
public class UpdateConferenceParticipantController {

    private final UpdateConferenceParticipantService updateService;

    @PostMapping("/{conferenceId}/actions/update")
    public UpdateConferenceParticipantResponseDto updateParticipant(
            @PathVariable String conferenceId,
            @RequestBody UpdateConferenceParticipantRequestDto request) {
        return updateService.updateParticipant(conferenceId, request);
    }
}
