package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.portal.PortalPendingUpdateDto;
import com.qiaben.ciyex.dto.portal.PortalUpdateRequest;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.portal.PortalReviewService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling patient updates that require EHR staff review/approval
 */
@RestController
@RequestMapping("/api/portal/review")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS },
    allowCredentials = "true"  
)
public class PortalReviewController {

    private final PortalReviewService reviewService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PortalUserRepository portalUserRepository;

    /**
     * Extract patient ID from JWT token
     */
    private Long extractPatientIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalStateException("Missing or invalid Authorization header");
        }
        
        String token = authHeader.substring(7);
        try {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            if (userId != null) return userId;

            String email = jwtTokenUtil.getEmailFromToken(token);
            return portalUserRepository.findByEmail(email)
                    .map(PortalUser::getId)
                    .orElseThrow(() -> new IllegalStateException("No user found for email: " + email));
        } catch (Exception e) {
            log.error("❌ Token validation failed", e);
            throw new IllegalStateException("Invalid or expired token");
        }
    }

    // =============================================================================
    // PATIENT ENDPOINTS - Submit updates for review
    // =============================================================================

    /**
     * POST /api/portal/review/submit - Patient submits any type of update for review
     */
    @PreAuthorize("hasRole('PATIENT')")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<String>> submitUpdate(
            HttpServletRequest request,
            @RequestBody PortalUpdateRequest updateRequest) {
        try {
            Long patientId = extractPatientIdFromToken(request);
            
            Long updateId = reviewService.submitForReview(patientId, updateRequest);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Update submitted for review. Staff will review your changes.")
                    .data("UPDATE_ID_" + updateId)
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to submit update for review", e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * GET /api/portal/review/status - Patient checks status of their pending updates
     */
    @PreAuthorize("hasRole('PATIENT')")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<PortalPendingUpdateDto>>> getMyUpdateStatus(HttpServletRequest request) {
        try {
            Long patientId = extractPatientIdFromToken(request);
            List<PortalPendingUpdateDto> updates = reviewService.getPatientUpdates(patientId);
            
            return ResponseEntity.ok(ApiResponse.<List<PortalPendingUpdateDto>>builder()
                    .success(true)
                    .message("Update status retrieved successfully")
                    .data(updates)
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to get update status", e);
            return ResponseEntity.badRequest().body(ApiResponse.<List<PortalPendingUpdateDto>>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    // =============================================================================
    // EHR STAFF ENDPOINTS - Review and approve/reject updates
    // =============================================================================

    /**
     * GET /api/portal/review/pending - EHR staff gets all pending updates
     */
    @PreAuthorize("hasRole('EHR_STAFF') or hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PortalPendingUpdateDto>>> getPendingReviews() {
        try {
            List<PortalPendingUpdateDto> pendingUpdates = reviewService.getAllPendingUpdates();
            
            return ResponseEntity.ok(ApiResponse.<List<PortalPendingUpdateDto>>builder()
                    .success(true)
                    .message("Pending updates retrieved successfully")
                    .data(pendingUpdates)
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to get pending reviews", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<List<PortalPendingUpdateDto>>builder()
                            .success(false)
                            .message("Failed to retrieve pending updates")
                            .build());
        }
    }

    /**
     * PUT /api/portal/review/approve/{id} - EHR staff approves an update
     */
    @PreAuthorize("hasRole('EHR_STAFF') or hasRole('ADMIN')")
    @PutMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<String>> approveUpdate(
            @PathVariable Long id,
            @RequestParam(required = false) String approverNotes,
            HttpServletRequest request) {
        try {
            // Get approver info from JWT
            String token = request.getHeader("Authorization").substring(7);
            String approverEmail = jwtTokenUtil.getEmailFromToken(token);
            
            reviewService.approveUpdate(id, approverEmail, approverNotes);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Update approved and merged into EHR system")
                    .data("APPROVED")
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to approve update", e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }

    /**
     * PUT /api/portal/review/reject/{id} - EHR staff rejects an update
     */
    @PreAuthorize("hasRole('EHR_STAFF') or hasRole('ADMIN')")
    @PutMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<String>> rejectUpdate(
            @PathVariable Long id,
            @RequestParam String reason,
            HttpServletRequest request) {
        try {
            // Get approver info from JWT
            String token = request.getHeader("Authorization").substring(7);
            String approverEmail = jwtTokenUtil.getEmailFromToken(token);
            
            reviewService.rejectUpdate(id, approverEmail, reason);
            
            return ResponseEntity.ok(ApiResponse.<String>builder()
                    .success(true)
                    .message("Update rejected")
                    .data("REJECTED")
                    .build());
        } catch (Exception e) {
            log.error("❌ Failed to reject update", e);
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build());
        }
    }
}