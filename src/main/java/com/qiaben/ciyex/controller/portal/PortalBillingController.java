package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.service.portal.PortalBillingService;
import com.qiaben.ciyex.service.InvoiceService;
import com.qiaben.ciyex.service.VitalsService;
import com.qiaben.ciyex.util.JwtTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own billing/invoices
 */
@RestController
@RequestMapping("/api/portal/billing")
@RequiredArgsConstructor
@CrossOrigin(
    origins = { "http://localhost:3000", "http://127.0.0.1:3000", "http://localhost:3001", "http://127.0.0.1:3001" },
    allowedHeaders = "*",
    methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS },
    allowCredentials = "true"
)
public class PortalBillingController {

    private final PortalBillingService billingService;
    private final InvoiceService sharedInvoiceService;
    private final VitalsService sharedVitalsService; // For getting EHR patient ID mapping
    private final JwtTokenUtil jwtUtil;

    /**
     * Get recent invoices for the currently logged-in patient
     * Endpoint: GET /api/portal/billing/recent
     */
    @GetMapping("/recent")
    public ApiResponse<List<InvoiceDto>> getRecentInvoices(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build();
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            return billingService.getRecentInvoices(userId);
        } catch (Exception e) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Invalid token")
                    .build();
        }
    }

    /**
     * Get all invoices for the currently logged-in patient
     * Endpoint: GET /api/portal/billing
     */
    @GetMapping
    public ApiResponse<List<InvoiceDto>> getAllInvoices(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Unauthorized - missing token")
                    .build();
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            return billingService.getAllInvoices(userId);
        } catch (Exception e) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Invalid token")
                    .build();
        }
    }

    /**
     * Extract Bearer token from Authorization header
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Get invoices for the currently logged-in portal patient (same as /api/billing/my but through portal proxy)
     * Endpoint: GET /api/portal/billing/my
     */
    @GetMapping("/my")
    public ApiResponse<List<InvoiceDto>> getMyInvoices(
            @RequestHeader(value = "x-org-id", required = false) Long orgId,
            Authentication authentication) {

        String email = authentication.getName();
        Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(email, orgId);

        if (ehrPatientId == null) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Patient record not linked to EHR")
                    .data(null)
                    .build();
        }

        // Get all invoices for this patient using the shared service
        List<InvoiceDto> invoices = sharedInvoiceService.getAllByPatient(orgId, ehrPatientId);

        return ApiResponse.<List<InvoiceDto>>builder()
                .success(true)
                .message("Patient invoices retrieved")
                .data(invoices)
                .build();
    }
}