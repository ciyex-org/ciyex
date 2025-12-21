package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.service.KeycloakAdminService;
import com.qiaben.ciyex.service.PracticeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test Controller for Token Expiry Functionality
 * 
 * This controller provides endpoints to test and verify the token expiry
 * functionality is working correctly with Keycloak integration.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TokenExpiryTestController {

    private final KeycloakAdminService keycloakAdminService;
    private final PracticeService practiceService;

    /**
     * GET /api/test/token-expiry/status
     * Returns comprehensive status of token expiry system
     */
    @GetMapping("/token-expiry/status")
    public ResponseEntity<ApiResponse<TokenExpiryStatusResponse>> getTokenExpiryStatus() {
        try {
            log.info("Testing token expiry system status");
            
            TokenExpiryStatusResponse response = new TokenExpiryStatusResponse();
            response.setTimestamp(LocalDateTime.now());
            
            // Test Keycloak connection
            boolean keycloakConnected = keycloakAdminService.testConnection();
            response.setKeycloakConnected(keycloakConnected);
            
            // Get current practice settings
            try {
                var practicesResponse = practiceService.getAllPractices();
                if (practicesResponse.getData() != null && !practicesResponse.getData().isEmpty()) {
                    var practice = practicesResponse.getData().get(0);
                    response.setCurrentTokenExpiryMinutes(practice.getTokenExpiryMinutes() != null ? 
                        practice.getTokenExpiryMinutes() : 5);
                    response.setPracticeConfigured(true);
                } else {
                    response.setPracticeConfigured(false);
                    response.setCurrentTokenExpiryMinutes(5); // Default
                }
            } catch (Exception e) {
                log.error("Error getting practice settings", e);
                response.setPracticeConfigured(false);
                response.setCurrentTokenExpiryMinutes(5);
            }
            
            // Overall system status
            response.setSystemReady(keycloakConnected && response.isPracticeConfigured());
            
            // Add recommendations
            Map<String, String> recommendations = new HashMap<>();
            if (!keycloakConnected) {
                recommendations.put("keycloak", "Check Keycloak connection and admin credentials");
            }
            if (!response.isPracticeConfigured()) {
                recommendations.put("practice", "Configure at least one practice in the system");
            }
            if (response.getCurrentTokenExpiryMinutes() < 5 || response.getCurrentTokenExpiryMinutes() > 30) {
                recommendations.put("expiry", "Token expiry should be between 5-30 minutes");
            }
            response.setRecommendations(recommendations);
            
            return ResponseEntity.ok(
                ApiResponse.<TokenExpiryStatusResponse>builder()
                    .success(true)
                    .message("Token expiry system status retrieved")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error getting token expiry status", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.<TokenExpiryStatusResponse>builder()
                    .success(false)
                    .message("Failed to get system status: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * POST /api/test/token-expiry/simulate
     * Simulates updating token expiry without actually changing settings
     */
    @PostMapping("/token-expiry/simulate")
    public ResponseEntity<ApiResponse<SimulationResponse>> simulateTokenExpiryUpdate(
            @RequestBody SimulationRequest request) {
        try {
            log.info("Simulating token expiry update to {} minutes", request.getMinutes());
            
            // Validate input
            if (request.getMinutes() < 5 || request.getMinutes() > 30) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.<SimulationResponse>builder()
                        .success(false)
                        .message("Token expiry must be between 5 and 30 minutes")
                        .data(null)
                        .build());
            }
            
            SimulationResponse response = new SimulationResponse();
            response.setRequestedMinutes(request.getMinutes());
            response.setTimestamp(LocalDateTime.now());
            
            // Test Keycloak connection (without updating)
            boolean keycloakReady = keycloakAdminService.testConnection();
            response.setKeycloakReady(keycloakReady);
            
            // Check practice configuration
            try {
                var practicesResponse = practiceService.getAllPractices();
                response.setPracticeReady(practicesResponse.getData() != null && 
                    !practicesResponse.getData().isEmpty());
            } catch (Exception e) {
                response.setPracticeReady(false);
            }
            
            // Calculate expected results
            response.setWouldSucceed(keycloakReady && response.isPracticeReady());
            
            // Add simulation details
            Map<String, String> simulationDetails = new HashMap<>();
            simulationDetails.put("keycloak_update", keycloakReady ? "Would succeed" : "Would fail - connection issue");
            simulationDetails.put("practice_update", response.isPracticeReady() ? "Would succeed" : "Would fail - no practice configured");
            simulationDetails.put("token_seconds", String.valueOf(request.getMinutes() * 60));
            simulationDetails.put("sso_timeout", String.valueOf(request.getMinutes() * 60));
            simulationDetails.put("max_lifespan", String.valueOf(request.getMinutes() * 60 * 2));
            response.setSimulationDetails(simulationDetails);
            
            return ResponseEntity.ok(
                ApiResponse.<SimulationResponse>builder()
                    .success(true)
                    .message("Simulation completed successfully")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error during simulation", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.<SimulationResponse>builder()
                    .success(false)
                    .message("Simulation failed: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    /**
     * GET /api/test/token-expiry/keycloak-connection
     * Tests Keycloak connection specifically
     */
    @GetMapping("/token-expiry/keycloak-connection")
    public ResponseEntity<ApiResponse<KeycloakConnectionResponse>> testKeycloakConnection() {
        try {
            log.info("Testing Keycloak connection");
            
            KeycloakConnectionResponse response = new KeycloakConnectionResponse();
            response.setTimestamp(LocalDateTime.now());
            
            long startTime = System.currentTimeMillis();
            boolean connected = keycloakAdminService.testConnection();
            long responseTime = System.currentTimeMillis() - startTime;
            
            response.setConnected(connected);
            response.setResponseTimeMs(responseTime);
            response.setStatus(connected ? "Connected" : "Failed");
            
            return ResponseEntity.ok(
                ApiResponse.<KeycloakConnectionResponse>builder()
                    .success(true)
                    .message(connected ? "Keycloak connection successful" : "Keycloak connection failed")
                    .data(response)
                    .build()
            );
        } catch (Exception e) {
            log.error("Error testing Keycloak connection", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.<KeycloakConnectionResponse>builder()
                    .success(false)
                    .message("Connection test failed: " + e.getMessage())
                    .data(null)
                    .build());
        }
    }

    // DTOs
    @Data
    public static class TokenExpiryStatusResponse {
        private LocalDateTime timestamp;
        private boolean keycloakConnected;
        private boolean practiceConfigured;
        private boolean systemReady;
        private Integer currentTokenExpiryMinutes;
        private Map<String, String> recommendations;
    }

    @Data
    public static class SimulationRequest {
        private Integer minutes;
    }

    @Data
    public static class SimulationResponse {
        private LocalDateTime timestamp;
        private Integer requestedMinutes;
        private boolean keycloakReady;
        private boolean practiceReady;
        private boolean wouldSucceed;
        private Map<String, String> simulationDetails;
    }

    @Data
    public static class KeycloakConnectionResponse {
        private LocalDateTime timestamp;
        private boolean connected;
        private String status;
        private long responseTimeMs;
    }
}