package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxSessionListResponseDto;
import com.qiaben.ciyex.service.telnyx.video.TelnyxSessionListResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class TelnyxSessionListResponseController
{
    private final TelnyxSessionListResponseService Service;

    @GetMapping("/{roomId}/sessions")
    public ResponseEntity<TelnyxSessionListResponseDto> listSessions(
            @PathVariable String roomId,
            @RequestParam(name = "filter[date_created_at][gte]", required = false) String createdGte,
            @RequestParam(name = "include_participants", defaultValue = "false") Boolean includeParticipants,
            @RequestParam(name = "page[size]", defaultValue = "20") Integer pageSize,
            @RequestParam(name = "page[number]", defaultValue = "1") Integer pageNumber
    ) {
        TelnyxSessionListResponseDto response = Service.listSessions(
                roomId, createdGte, includeParticipants, pageSize, pageNumber);
        return ResponseEntity.ok(response);
    }
}
