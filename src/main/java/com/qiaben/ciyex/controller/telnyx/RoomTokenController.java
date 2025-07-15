package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.GenerateTokenRequest;
import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.RefreshTokenRequest;
import com.qiaben.ciyex.dto.telnyx.RoomTokenDto.RoomTokenResponse;
import com.qiaben.ciyex.service.telnyx.RoomTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/rooms")
@RequiredArgsConstructor
public class RoomTokenController {

    private final RoomTokenService svc;

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
