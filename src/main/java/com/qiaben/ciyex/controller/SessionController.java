package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.KeycloakTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
@Slf4j
public class SessionController {

    private final KeycloakTokenService keycloakTokenService;
    
    public SessionController(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    @PostMapping("/keep-alive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> keepAlive(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> body) {
        
        String refreshToken = null;
        if (body != null) {
            refreshToken = body.get("refreshToken");
        }
        
        Map<String, Object> result = Map.of("timestamp", System.currentTimeMillis());
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // Check if token is expiring soon and refresh if needed
            if (keycloakTokenService.isTokenExpiringSoon(token) && refreshToken != null) {
                Map<String, Object> refreshResult = keycloakTokenService.refreshToken(refreshToken);
                if (refreshResult != null) {
                    result = Map.of(
                        "timestamp", System.currentTimeMillis(),
                        "newToken", refreshResult.get("access_token"),
                        "refreshToken", refreshResult.get("refresh_token")
                    );
                }
            }
        }
        
        return ResponseEntity.ok(
            ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Session active")
                .data(result)
                .build()
        );
    }
}