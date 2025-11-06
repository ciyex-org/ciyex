package com.qiaben.ciyex.controller.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.service.portal.PortalBillingService;
import com.qiaben.ciyex.service.InvoiceService;
import com.qiaben.ciyex.service.VitalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for portal patients to view their own billing/invoices
 */
@RestController
@RequestMapping("/api/fhir/portal/billing")
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

    /**
     * Get recent invoices for the currently logged-in patient
     * Endpoint: GET /api/portal/billing/recent
     */
    @GetMapping("/recent")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<InvoiceDto>> getRecentInvoices(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Get Keycloak user UUID from authentication
            String keycloakUserId = authentication.getName();
            return billingService.getRecentInvoices(keycloakUserId);
        } catch (Exception e) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve invoices: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Get all invoices for the currently logged-in patient
     * Endpoint: GET /api/portal/billing
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<InvoiceDto>> getAllInvoices(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Unauthorized - not authenticated")
                    .build();
        }

        try {
            // Get Keycloak user UUID from authentication
            String keycloakUserId = authentication.getName();
            return billingService.getAllInvoices(keycloakUserId);
        } catch (Exception e) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve invoices: " + e.getMessage())
                    .build();
        }
    }


    /**
     * Get invoices for the currently logged-in portal patient (same as /api/billing/my but through portal proxy)
     * Endpoint: GET /api/portal/billing/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('PATIENT') or hasRole('PATIENT')")
    public ApiResponse<List<InvoiceDto>> getMyInvoices(
            Authentication authentication) {

        // Get Keycloak user UUID from authentication
        String keycloakUserId = authentication.getName();
        Long ehrPatientId = sharedVitalsService.getEhrPatientIdFromPortalUserEmail(keycloakUserId);

        if (ehrPatientId == null) {
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Patient record not linked to EHR")
                    .data(null)
                    .build();
        }

        // Get all invoices for this patient using the shared service
        List<InvoiceDto> invoices = sharedInvoiceService.getAllByPatient(ehrPatientId);

        return ApiResponse.<List<InvoiceDto>>builder()
                .success(true)
                .message("Patient invoices retrieved")
                .data(invoices)
                .build();
    }
}