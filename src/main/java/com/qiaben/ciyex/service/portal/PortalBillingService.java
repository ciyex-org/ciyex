package com.qiaben.ciyex.service.portal;

import com.qiaben.ciyex.dto.portal.ApiResponse;
import com.qiaben.ciyex.dto.InvoiceDto;
import com.qiaben.ciyex.entity.portal.PortalUser;
import com.qiaben.ciyex.enums.PortalStatus;
import com.qiaben.ciyex.repository.InvoiceRepository;
import com.qiaben.ciyex.repository.portal.PortalUserRepository;
import com.qiaben.ciyex.service.InvoiceService;
import com.qiaben.ciyex.service.VitalsService;
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
    private final VitalsService vitalsService;

    /**
     * Get all invoices for a portal patient by email
     */
    public ApiResponse<List<InvoiceDto>> getAllInvoices(String userEmail) {
        try {
            // Get EHR patient ID from portal user email
            Long ehrPatientId = vitalsService.getEhrPatientIdFromPortalUserEmail(userEmail);

            if (ehrPatientId == null) {
                return ApiResponse.<List<InvoiceDto>>builder()
                        .success(false)
                        .message("Patient record not linked to EHR")
                        .build();
            }

            // Get all invoices for this patient
            List<InvoiceDto> invoices = invoiceService.getAllByPatient(ehrPatientId);

            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(true)
                    .message("Patient invoices retrieved successfully")
                    .data(invoices)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving invoices for user: {}", userEmail, e);
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve invoices")
                    .build();
        }
    }

    /**
     * Get recent invoices for a portal patient by email (last 10 records)
     */
    public ApiResponse<List<InvoiceDto>> getRecentInvoices(String userEmail) {
        try {
            ApiResponse<List<InvoiceDto>> response = getAllInvoices(userEmail);
            if (response.isSuccess() && response.getData() != null) {
                List<InvoiceDto> recentInvoices = response.getData()
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
                        .data(recentInvoices)
                        .build();
            } else {
                return response;
            }

        } catch (Exception e) {
            log.error("Error retrieving recent invoices for user: {}", userEmail, e);
            return ApiResponse.<List<InvoiceDto>>builder()
                    .success(false)
                    .message("Failed to retrieve recent invoices")
                    .build();
        }
    }
}