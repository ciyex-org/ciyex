// src/main/java/com/qiaben/ciyex/service/AllergyIntoleranceService.java
package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
import com.qiaben.ciyex.dto.core.integration.RequestContext;
import com.qiaben.ciyex.entity.AllergyIntolerance;
import com.qiaben.ciyex.repository.AllergyIntoleranceRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AllergyIntoleranceService {

    private final AllergyIntoleranceRepository repo;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public AllergyIntoleranceService(AllergyIntoleranceRepository repo,
                                     ExternalStorageResolver storageResolver,
                                     OrgIntegrationConfigProvider configProvider) {
        this.repo = repo;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    /* ---------------------- Top-level ops ---------------------- */

    @Transactional
    public AllergyIntoleranceDto create(AllergyIntoleranceDto dto) {
        Long orgId = requireOrg("create");
        if (dto.getPatientId() == null) throw new IllegalArgumentException("patientId is required");
        dto.setOrgId(orgId);

        String now = LocalDateTime.now().toString();
        List<AllergyIntolerance> rows = new ArrayList<>();

        if (dto.getAllergiesList() != null) {
            for (var it : dto.getAllergiesList()) {
                AllergyIntolerance row = AllergyIntolerance.builder()
                        .orgId(orgId)
                        .patientId(dto.getPatientId())
                        .allergyName(it.getAllergyName())
                        .reaction(it.getReaction())
                        .severity(it.getSeverity())
                        .status(it.getStatus())
                        .createdDate(now)
                        .lastModifiedDate(now)
                        .build();
                rows.add(repo.save(row));
            }
        }

        // Optional: sync an external snapshot (e.g., FHIR List)
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && !rows.isEmpty()) {
            ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);

            // For external sync we include top-level patientId (used for FHIR title parsing, etc.)
            AllergyIntoleranceDto snapshot = toDto(orgId, dto.getPatientId(), rows, true);
            String externalId = ext.create(snapshot);

            for (AllergyIntolerance r : rows) {
                r.setExternalId(externalId);
                repo.save(r);
            }
        }

        // API response: omit top-level patientId (kept only inside items)
        return toDto(orgId, dto.getPatientId(), rows, false);
    }

    @Transactional(readOnly = true)
    public AllergyIntoleranceDto getByPatientId(Long patientId) {
        Long orgId = requireOrg("getByPatientId");
        List<AllergyIntolerance> rows =
                repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

        if (rows.isEmpty())
            throw new RuntimeException("No allergies found for patientId=" + patientId);

        // API response: omit top-level patientId
        AllergyIntoleranceDto dto = toDto(orgId, patientId, rows, false);

        // Optionally refresh external id from external storage
        if (rows.get(0).getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                AllergyIntoleranceDto fromExt = ext.get(rows.get(0).getExternalId());
                if (fromExt != null && fromExt.getExternalId() != null) dto.setExternalId(fromExt.getExternalId());
            }
        }
        return dto;
    }

    @Transactional
    public AllergyIntoleranceDto updateByPatientId(Long patientId, AllergyIntoleranceDto dto) {
        Long orgId = requireOrg("updateByPatientId");
        // Replace strategy: delete all, then create new rows from payload
        repo.deleteAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        dto.setOrgId(orgId);
        dto.setPatientId(patientId);
        return create(dto);
    }

    @Transactional
    public void deleteByPatientId(Long patientId) {
        Long orgId = requireOrg("deleteByPatientId");
        List<AllergyIntolerance> rows =
                repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        if (rows.isEmpty()) return;

        String externalId = rows.get(0).getExternalId();
        repo.deleteAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                ext.delete(externalId);
            }
        }
    }

    /* ---------------------- Item-level ops (used by /{patientId}/{intoleranceId}) ---------------------- */

    @Transactional(readOnly = true)
    public AllergyIntoleranceDto.AllergyItem getItem(Long patientId, Long intoleranceId) {
        Long orgId = requireOrg("getItem");
        return repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId)).stream()
                .filter(r -> r.getId().equals(intoleranceId))
                .findFirst()
                .map(this::toItem)
                .orElseThrow(() -> new RuntimeException(
                        "Allergy not found for patientId=" + patientId + " intoleranceId=" + intoleranceId));
    }

    @Transactional
    public AllergyIntoleranceDto.AllergyItem updateItem(Long patientId, Long intoleranceId,
                                                        AllergyIntoleranceDto.AllergyItem patch) {
        Long orgId = requireOrg("updateItem");
        List<AllergyIntolerance> rows =
                repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

        AllergyIntolerance row = rows.stream()
                .filter(r -> r.getId().equals(intoleranceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Allergy not found id=" + intoleranceId));

        if (patch.getAllergyName() != null) row.setAllergyName(patch.getAllergyName());
        if (patch.getReaction() != null)    row.setReaction(patch.getReaction());
        if (patch.getSeverity() != null)    row.setSeverity(patch.getSeverity());
        if (patch.getStatus() != null)      row.setStatus(patch.getStatus());
        row.setLastModifiedDate(LocalDateTime.now().toString());
        repo.save(row);

        // Re-sync external snapshot if present
        if (row.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                List<AllergyIntolerance> fresh =
                        repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

                // For external sync we include top-level patientId
                ext.update(toDto(orgId, patientId, fresh, true), row.getExternalId());
            }
        }
        return toItem(row);
    }

    @Transactional
    public void deleteItem(Long patientId, Long intoleranceId) {
        Long orgId = requireOrg("deleteItem");
        List<AllergyIntolerance> rows =
                repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        String externalId = rows.stream().findFirst().map(AllergyIntolerance::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientIdAndOrgIdText(
                String.valueOf(intoleranceId), String.valueOf(patientId), String.valueOf(orgId));
        if (n == 0) throw new RuntimeException("Delete failed: not found");

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                List<AllergyIntolerance> fresh =
                        repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
                if (fresh.isEmpty()) ext.delete(externalId);
                else {
                    // For external sync we include top-level patientId
                    ext.update(toDto(orgId, patientId, fresh, true), externalId);
                }
            }
        }
    }

    /* ---------------------- Search all (by org, grouped by patient) ---------------------- */

    @Transactional(readOnly = true)
    public ApiResponse<List<AllergyIntoleranceDto>> searchAll() {
        Long orgId = requireOrg("searchAll");

        List<AllergyIntolerance> all = repo.findByOrgIdText(String.valueOf(orgId));
        Map<Long, List<AllergyIntolerance>> byPatient =
                all.stream().collect(Collectors.groupingBy(AllergyIntolerance::getPatientId));

        List<AllergyIntoleranceDto> dtos = new ArrayList<>();
        for (var e : byPatient.entrySet()) {
            // API response: omit top-level patientId
            dtos.add(toDto(orgId, e.getKey(), e.getValue(), false));
        }

        return ApiResponse.<List<AllergyIntoleranceDto>>builder()
                .success(true)
                .message("Allergy Intolerances retrieved successfully")
                .data(dtos)
                .build();
    }

    /* ---------------------- Helpers ---------------------- */

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    /**
     * Build DTO with control over whether to include the top-level patientId.
     * We include it only for internal/external sync needs; API responses omit it.
     */
    private AllergyIntoleranceDto toDto(Long orgId, Long patientId, List<AllergyIntolerance> rows,
                                        boolean includeTopLevelPatientId) {
        AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
        dto.setOrgId(orgId);
        if (includeTopLevelPatientId) {
            dto.setPatientId(patientId);
        }
        if (!rows.isEmpty()) {
            dto.setExternalId(rows.get(0).getExternalId());
            AllergyIntoleranceDto.Audit a = new AllergyIntoleranceDto.Audit();
            a.setCreatedDate(rows.get(0).getCreatedDate());
            a.setLastModifiedDate(rows.get(0).getLastModifiedDate());
            dto.setAudit(a);
        }
        dto.setAllergiesList(rows.stream().map(this::toItem).collect(Collectors.toList()));
        return dto;
    }

    private AllergyIntoleranceDto.AllergyItem toItem(AllergyIntolerance r) {
        AllergyIntoleranceDto.AllergyItem it = new AllergyIntoleranceDto.AllergyItem();
        it.setId(r.getId());
        it.setAllergyName(r.getAllergyName());
        it.setReaction(r.getReaction());
        it.setSeverity(r.getSeverity());
        it.setStatus(r.getStatus());
        it.setPatientId(r.getPatientId()); // <-- patientId only inside each list item
        return it;
    }
}
