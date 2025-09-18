package com.qiaben.ciyex.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StripeController {

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @GetMapping("/api/stripe/config")
    public Map<String, String> getStripeConfig() {
        return Map.of("publishableKey", publishableKey);
    }
}
