package com.qiaben.ciyex.controller.fhir;

import com.qiaben.ciyex.dto.fhir.OpenEmrTokenRequest;
import com.qiaben.ciyex.service.fhir.OpenEmrAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/openemr")
public class OpenEmrAuthController {
    private final OpenEmrAuthService authService;

    @Autowired
    public OpenEmrAuthController(OpenEmrAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/token")
    public String getAccessToken(@RequestBody(required = false) OpenEmrTokenRequest request) throws Exception {
        if (request == null) request = new OpenEmrTokenRequest();
        return authService.getCachedAccessToken();
    }
}

