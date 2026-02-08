package org.ciyex.ehr.controller.portal;

import org.ciyex.ehr.dto.portal.ApiResponse;
import org.ciyex.ehr.dto.portal.PortalUserDto;
import org.ciyex.ehr.service.portal.PortalApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for EHR staff to manage portal user approvals
 */
@RestController
@RequestMapping("/api/portal/approvals")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalApprovalController {

    private final PortalApprovalService portalApprovalService;

    /**
     * Get all pending portal users waiting for approval
     * GET /api/portal/approvals/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PortalUserDto>>> getPendingUsers() {
        return ResponseEntity.ok(portalApprovalService.getPendingUsers());
    }

    /**
     * Approve a portal user and sync to EHR tenant
     * PUT /api/portal/approvals/approve/{id}
     */
    @PutMapping("/approve/{id}")
    public ResponseEntity<ApiResponse<PortalUserDto>> approveUser(
            @PathVariable Long id,
            @RequestParam(required = false) Long approvedBy) {
        
        // In a real app, you'd get approvedBy from JWT token
        Long approverUserId = approvedBy != null ? approvedBy : 1L;
        
        return ResponseEntity.ok(portalApprovalService.approveUser(id, approverUserId));
    }

    /**
     * Reject a portal user
     * PUT /api/portal/approvals/reject/{id}?reason=...
     */
    @PutMapping("/reject/{id}")
    public ResponseEntity<ApiResponse<PortalUserDto>> rejectUser(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Long rejectedBy) {
        
        // In a real app, you'd get rejectedBy from JWT token
        Long rejecterUserId = rejectedBy != null ? rejectedBy : 1L;
        String rejectionReason = reason != null ? reason : "No reason provided";
        
        return ResponseEntity.ok(portalApprovalService.rejectUser(id, rejectionReason, rejecterUserId));
    }

    /**
     * Get the status of a patient linking request
     * GET /api/portal/approvals/link-status/{portalUserId}
     */
    @GetMapping("/link-status/{portalUserId}")
    public ResponseEntity<ApiResponse<String>> getLinkStatus(@PathVariable("portalUserId") Long portalUserId) {
        return ResponseEntity.ok(portalApprovalService.getLinkStatus(portalUserId));
    }

    /**
     * Create a patient linking request
     * POST /api/portal/approvals/link-patient
     */
    @PostMapping("/link-patient")
    public ResponseEntity<ApiResponse<String>> linkPatient(@RequestBody LinkPatientRequest request) {
        return ResponseEntity.ok(portalApprovalService.linkPatient(
            request.getPortalUserId(),
            request.getEhrPatientId(),
            request.getRequestReason()
        ));
    }

    /**
     * DTO for patient linking request
     */
    public static class LinkPatientRequest {
        private Long portalUserId;
        private Long ehrPatientId;
        private String requestReason;

        public LinkPatientRequest() {}

        public LinkPatientRequest(Long portalUserId, Long ehrPatientId, String requestReason) {
            this.portalUserId = portalUserId;
            this.ehrPatientId = ehrPatientId;
            this.requestReason = requestReason;
        }

        public Long getPortalUserId() { return portalUserId; }
        public void setPortalUserId(Long portalUserId) { this.portalUserId = portalUserId; }

        public Long getEhrPatientId() { return ehrPatientId; }
        public void setEhrPatientId(Long ehrPatientId) { this.ehrPatientId = ehrPatientId; }

        public String getRequestReason() { return requestReason; }
        public void setRequestReason(String requestReason) { this.requestReason = requestReason; }
    }
}