package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.StripeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stripe/config")
public class StripeConfigController {

    private final StripeService stripeService;

    public StripeConfigController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @GetMapping("/{orgId}")
    public Map<String, Object> getConfig(@PathVariable Long orgId) {
        String pk = stripeService.getPublishableKey(orgId);
        Map<String, Object> resp = new HashMap<>();
        if (pk != null) {
            resp.put("publishableKey", pk);
            resp.put("hasConfig", true);
        } else {
            resp.put("hasConfig", false);
        }
        return resp;
    }
}
