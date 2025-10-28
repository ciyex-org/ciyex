package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.InvoiceRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalBillingService {

    private final InvoiceRepository invoiceRepository;
    private final PortalUserRepository userRepository;
    private final InvoiceService invoiceService;

    /**
     * Get all invoices for a portal patient
     */
    public ApiResponse<List<InvoiceDto>> getAllInvoices(Long portalUserId) {
        try {
            PortalUser portalUser = userRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();
            Long orgId = portalUser.getOrgId();

            // Get all invoices for this patient
            List<InvoiceDto> invoices = invoiceService.getAllByPatient(orgId, ehrPatientId);

            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(true)
                    .message("Patient invoices retrieved successfully")
                    .data(invoices)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving invoices for user: {}", portalUserId, e);
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve invoices")
                    .build();
        }
    }

    /**
     * Get recent invoices for a portal patient (last 10 records)
     */
    public ApiResponse<List<InvoiceDto>> getRecentInvoices(Long portalUserId) {
        try {
            PortalUser portalUser = userRepository.findById(portalUserId)
                    .orElse(null);

            if (portalUser == null) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("Portal user not found")
                        .build();
            }

            if (portalUser.getStatus() != PortalStatus.APPROVED) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("User not approved")
                        .build();
            }

            // Get the EHR patient ID from portal patient
            if (portalUser.getPortalPatient() == null || portalUser.getPortalPatient().getEhrPatientId() == null) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            Long ehrPatientId = portalUser.getPortalPatient().getEhrPatientId();
            Long orgId = portalUser.getOrgId();

            // Get all invoices for this patient and take the most recent 10
            List<InvoiceDto> invoices = invoiceService.getAllByPatient(orgId, ehrPatientId)
                    .stream()
                    .sorted((a, b) -> {
                        // Sort by issue date descending (most recent first)
                        if (a.getIssueDate() != null && b.getIssueDate() != null) {
                            return b.getIssueDate().compareTo(a.getIssueDate());
                        }
                        return 0;
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(true)
                    .message("Recent invoices retrieved successfully")
                    .data(invoices)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving recent invoices for user: {}", portalUserId, e);
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve recent invoices")
                    .build();
        }
    }
}