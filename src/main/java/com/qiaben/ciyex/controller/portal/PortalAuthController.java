package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalLoginRequest;
import com.qiaben.ciyex.dto.portal.PortalLoginResponse;
import com.qiaben.ciyex.dto.portal.PortalRegisterRequest;
import com.qiaben.ciyex.service.portal.PortalAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for portal user authentication (registration and login)
 */
@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    /**
     * Register a new portal user
     * POST /api/portal/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> register(@RequestBody PortalRegisterRequest request) {
        log.info("Portal user registration attempt: {}", request.getEmail());
        return ResponseEntity.ok(portalAuthService.register(request));
    }

    /**
     * Login portal user
     * POST /api/portal/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> login(@RequestBody PortalLoginRequest request) {
        log.info("Portal user login attempt: {}", request.getEmail());
        return ResponseEntity.ok(portalAuthService.login(request));
    }

    /**
     * Get user profile (requires authentication)
     * GET /api/portal/auth/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<PortalLoginResponse>> getProfile(@RequestParam Long userId) {
        return ResponseEntity.ok(portalAuthService.getProfile(userId));
    }
}