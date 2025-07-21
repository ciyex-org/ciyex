package com.qiaben.ciyex.controller.telnyx.video;

import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.GenerateTokenRequest;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.RefreshTokenRequest;
import com.qiaben.ciyex.dto.telnyx.video.TelnyxRoomTokenDto.RoomTokenResponse;
import com.qiaben.ciyex.service.telnyx.video.TelnyxRoomTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class TelnyxRoomTokenController {

    private final TelnyxRoomTokenService svc;

    @PostMapping("/{roomId}/tokens")
    public RoomTokenResponse generate(
            @PathVariable String roomId,
            @RequestBody GenerateTokenRequest body) {
        return svc.generate(roomId, body);
    }

    @PostMapping("/{roomId}/tokens/refresh")
    public RoomTokenResponse refresh(
            @PathVariable String roomId,
            @RequestBody RefreshTokenRequest body) {
        return svc.refresh(roomId, body);
    }
}
