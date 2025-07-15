package com.qiaben.ciyex.controller.telnyx;

import com.qiaben.ciyex.dto.telnyx.AccessTokenResponseDto;
import com.qiaben.ciyex.service.telnyx.AccessTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/telnyx/access-token")
@RequiredArgsConstructor
public class AccessTokenController {

    private final AccessTokenService tokenService;

    @PostMapping("/{id}")
    public AccessTokenResponseDto generate(@PathVariable("id") String credentialId) {
        return tokenService.createAccessToken(credentialId);
    }
}
