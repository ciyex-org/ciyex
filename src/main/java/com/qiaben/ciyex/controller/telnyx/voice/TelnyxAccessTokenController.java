package com.qiaben.ciyex.controller.telnyx.voice;

import com.qiaben.ciyex.dto.telnyx.voice.TelnyxAccessTokenResponseDto;
import com.qiaben.ciyex.service.telnyx.voice.TelnyxAccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/access-token")
@RequiredArgsConstructor
public class TelnyxAccessTokenController {

    private final TelnyxAccessTokenService tokenService;

    @PostMapping("/{id}")
    public TelnyxAccessTokenResponseDto generate(@PathVariable("id") String credentialId) {
        return tokenService.createAccessToken(credentialId);
    }
}
