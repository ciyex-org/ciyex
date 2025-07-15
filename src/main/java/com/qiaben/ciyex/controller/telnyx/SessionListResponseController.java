package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.SessionListResponseDto;
import com.qiaben.ciyex.service.telnyx.SessionListResponseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class SessionListResponseController
{
    private final SessionListResponseService Service;

    @GetMapping("/{roomId}/sessions")
    public ResponseEntity<SessionListResponseDto> listSessions(
            @PathVariable String roomId,
            @RequestParam(name = "filter[date_created_at][gte]", required = false) String createdGte,
            @RequestParam(name = "include_participants", defaultValue = "false") Boolean includeParticipants,
            @RequestParam(name = "page[size]", defaultValue = "20") Integer pageSize,
            @RequestParam(name = "page[number]", defaultValue = "1") Integer pageNumber
    ) {
        SessionListResponseDto response = Service.listSessions(
                roomId, createdGte, includeParticipants, pageSize, pageNumber);
        return ResponseEntity.ok(response);
    }
}
