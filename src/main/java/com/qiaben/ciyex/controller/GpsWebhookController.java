package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.service.GpsBillingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/gps")
@RequiredArgsConstructor
@Slf4j
public class GpsWebhookController {

    private final GpsBillingHistoryService billingHistoryService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleGpsWebhook(
            HttpServletRequest request, 
            @RequestBody String payload) {
        
        log.info("Received GPS webhook: {}", payload);
        
        try {
            // Parse GPS webhook payload
            // GPS typically sends form-encoded data
            String[] pairs = payload.split("&");
            String transactionId = null;
            String status = null;
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = keyValue[0];
                    String value = keyValue[1];
                    
                    switch (key) {
                        case "transactionid":
                            transactionId = value;
                            break;
                        case "response":
                            status = "1".equals(value) ? "SUCCESS" : "FAILED";
                            break;
                    }
                }
            }
            
            if (transactionId != null && status != null) {
                billingHistoryService.updateStatus(transactionId, status);
                log.info("Updated GPS transaction {} status to {}", transactionId, status);
            }
            
            return ResponseEntity.ok("✅ GPS Webhook received");
            
        } catch (Exception e) {
            log.error("Error processing GPS webhook: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("⚠️ Error processing webhook");
        }
    }
}