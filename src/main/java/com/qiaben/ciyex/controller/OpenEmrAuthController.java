package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.OpenEmrTokenRequest;
import com.qiaben.ciyex.dto.OpenEmrTokenResponse;
import com.qiaben.ciyex.service.OpenEmrAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openemr")
@RequiredArgsConstructor
public class OpenEmrAuthController {
    private final OpenEmrAuthService authService;

    @PostMapping("/token")
    public OpenEmrTokenResponse getAccessToken(@RequestBody(required = false) OpenEmrTokenRequest request) throws Exception {
        if (request == null) request = new OpenEmrTokenRequest();
        return authService.getAccessToken(request);
    }
}

