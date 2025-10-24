package com.qiaben.ciyex.controller;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.LabOrderDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.service.LabOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lab-order")
@Slf4j
public class LabOrderController {

    private final LabOrderService service;

    public LabOrderController(LabOrderService service) {
        this.service = service;
    }

    // ---- READ ALL for patient ----
    // Controller-only filtering to avoid SQL comparing varchar patient_id to bigint
    @GetMapping("/{patientId}")
    public ResponseEntity<ApiResponse<List<LabOrderDto>>> listForPatient(
            @RequestHeader(value = "X-Org-Id", required = false) String orgHeader,
            @PathVariable Long patientId) {
        List<Long> orgIds = parseOrgIds(orgHeader, null);
        try {
            seedRequestContextFirst(orgIds);

            ApiResponse<List<LabOrderDto>> all = service.getAll(orgIds);
            if (all == null) {
                return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                        .success(false)
                        .message("Unknown error retrieving lab orders")
                        .build());
            }
            if (!all.isSuccess()) {
                return ResponseEntity.ok(all);
            }

            List<LabOrderDto> filtered = (all.getData() == null ? List.<LabOrderDto>of() : all.getData())
                    .stream()
                    .filter(d -> d.getPatientId() != null && Objects.equals(d.getPatientId(), patientId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                    .success(true)
                    .message("Lab orders retrieved successfully")
                    .data(filtered)
                    .build());

        } catch (Exception e) {
            log.error("Failed to list lab orders for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<List<LabOrderDto>>builder()
                    .success(false)
                    .message("Failed to list lab orders: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---- READ one for patient ----
    // Fetch by ID only, then verify patientId in memory (no patient_id comparison in SQL)
    @GetMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<LabOrderDto>> getForPatient(
            @RequestHeader(value = "X-Org-Id", required = false) String orgHeader,
            @PathVariable Long patientId,
            @PathVariable Long id) {
        List<Long> orgIds = parseOrgIds(orgHeader, null);
        try {
            seedRequestContextFirst(orgIds);

            LabOrderDto dto = service.getById(id, orgIds);
            if (dto == null || dto.getPatientId() == null || !Objects.equals(dto.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                        .success(false)
                        .message("Lab order not found for the specified patient")
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(true)
                    .message("Lab order retrieved successfully")
                    .data(dto)
                    .build());

        } catch (Exception e) {
            log.error("Failed to read lab order {} for patient {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(false)
                    .message("Failed to retrieve lab order: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---- CREATE for patient ----
    @PostMapping("/{patientId}")
    public ResponseEntity<ApiResponse<LabOrderDto>> createForPatient(
            @RequestHeader(value = "X-Org-Id", required = false) String orgHeader,
            @PathVariable Long patientId,
            @RequestBody LabOrderDto dto) {
        List<Long> orgIds = parseOrgIds(orgHeader, null); // orgId deprecated, using tenantName from context
        try {
            seedRequestContextFirst(orgIds);

            if (dto.getPatientId() == null) {
                dto.setPatientId(patientId);
            } else if (!patientId.equals(dto.getPatientId())) {
                throw new IllegalArgumentException("patientId in path does not match patientId in payload");
            }

            LabOrderDto created = service.create(dto, orgIds);
            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(true)
                    .message("Lab order created successfully")
                    .data(created)
                    .build());
        } catch (Exception e) {
            log.error("Failed to create lab order for patient {}: {}", patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(false)
                    .message("Failed to create lab order: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---- UPDATE for patient ----
    // Verify patient match in controller using getById(..), then update by id only
    @PutMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<LabOrderDto>> updateForPatient(
            @RequestHeader(value = "X-Org-Id", required = false) String orgHeader,
            @PathVariable Long patientId,
            @PathVariable Long id,
            @RequestBody LabOrderDto dto) {
        List<Long> orgIds = parseOrgIds(orgHeader, null); // orgId deprecated, using tenantName from context
        try {
            seedRequestContextFirst(orgIds);

            LabOrderDto existing = service.getById(id, orgIds);
            if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                        .success(false)
                        .message("Lab order not found for the specified patient")
                        .build());
            }

            dto.setPatientId(patientId);
            LabOrderDto updated = service.update(id, dto, orgIds);
            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(true)
                    .message("Lab order updated successfully")
                    .data(updated)
                    .build());
        } catch (Exception e) {
            log.error("Failed to update lab order {} for patient {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<LabOrderDto>builder()
                    .success(false)
                    .message("Failed to update lab order: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // ---- DELETE for patient ----
    // Verify patient match in controller using getById(..), then delete by id only
    @DeleteMapping("/{patientId}/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteForPatient(
            @RequestHeader(value = "X-Org-Id", required = false) String orgHeader,
            @PathVariable Long patientId,
            @PathVariable Long id) {
        List<Long> orgIds = parseOrgIds(orgHeader, null);
        try {
            seedRequestContextFirst(orgIds);

            LabOrderDto existing = service.getById(id, orgIds);
            if (existing == null || existing.getPatientId() == null || !Objects.equals(existing.getPatientId(), patientId)) {
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .success(false)
                        .message("Lab order not found for the specified patient")
                        .build());
            }

            service.delete(id, orgIds);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Lab order deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete lab order {} for patient {}: {}", id, patientId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(false)
                    .message("Failed to delete lab order: " + e.getMessage())
                    .build());
        } finally {
            RequestContext.clear();
        }
    }

    // --- helpers ---
    private List<Long> parseOrgIds(String orgHeader, Long singleFallback) {
        List<Long> ids = new ArrayList<>();
        if (orgHeader != null && !orgHeader.isBlank()) {
            String[] parts = orgHeader.split("[,;]");
            for (String p : parts) {
                try {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) ids.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {}
            }
        }
        if (ids.isEmpty() && singleFallback != null) ids.add(singleFallback);
        return ids;
    }

    private void seedRequestContextFirst(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) return;
        Long first = orgIds.get(0);
        var ctx = RequestContext.get();
        if (ctx == null) {
            ctx = new RequestContext();
            RequestContext.set(ctx);
        }
        // Legacy orgId support - convert Long to String for tenantName
        ctx.setTenantName("practice_" + first);
    }
}
