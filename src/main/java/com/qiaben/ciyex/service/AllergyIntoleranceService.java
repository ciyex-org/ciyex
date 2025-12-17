package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.AllergyIntoleranceDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.AllergyIntolerance;
import com.qiaben.ciyex.repository.AllergyIntoleranceRepository;
import com.qiaben.ciyex.storage.ExternalStorage;
import com.qiaben.ciyex.storage.ExternalStorageResolver;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
        if (dto.getPatientId() == null) throw new IllegalArgumentException("patientId is required");

        String now = LocalDateTime.now().toString();
        List<AllergyIntolerance> rows = new ArrayList<>();

        if (dto.getAllergiesList() != null) {
            for (var it : dto.getAllergiesList()) {
                // Validate mandatory fields
                validateMandatoryFields(it);
                validateDates(it.getStartDate(), it.getEndDate());

                AllergyIntolerance row = AllergyIntolerance.builder()
                        .patientId(dto.getPatientId())
                        .allergyName(it.getAllergyName())
                        .reaction(it.getReaction())
                        .severity(it.getSeverity())
                        .status(it.getStatus())
                        .startDate(it.getStartDate())
                        .endDate(it.getEndDate())
                        .comments(it.getComments())       // NEW
                        .build();
                rows.add(repo.save(row));
            }
        }

        // Optional external sync with graceful fallback
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        String externalId = null;
        if (storageType != null && !rows.isEmpty()) {
            try {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                AllergyIntoleranceDto snapshot = toDto(dto.getPatientId(), rows, true);
                externalId = ext.create(snapshot);
                
                if (externalId != null) {
                    for (AllergyIntolerance r : rows) {
                        r.setFhirId(externalId);
                        r.setExternalId(externalId);
                        repo.save(r);
                    }
                    log.info("Successfully created allergy intolerance in external storage with externalId: {}", externalId);
                }
            } catch (Exception e) {
                log.warn("Failed to sync with external storage, falling back to local generation: {}", e.getMessage());
                // Fall back to auto-generation if external storage fails
                externalId = null;
            }
        }

        // Auto-generate externalId if not provided, no external storage, or external storage failed
        if (externalId == null) {
            externalId = "ALG-" + System.currentTimeMillis();
            for (AllergyIntolerance r : rows) {
                r.setFhirId(externalId);
                r.setExternalId(externalId);
                repo.save(r);
            }
            log.info("Auto-generated externalId: {}", externalId);
        }

        // API response: omit top-level patientId
        return toDto(dto.getPatientId(), rows, false);
    }

    @Transactional(readOnly = true)
    public AllergyIntoleranceDto getByPatientId(Long patientId) {
        List<AllergyIntolerance> rows = repo.findAllByPatientId(patientId);

        AllergyIntoleranceDto dto = toDto(patientId, rows, false);

        if (!rows.isEmpty() && rows.get(0).getExternalId() != null) {
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
        repo.deleteAllByPatientId(patientId);
        dto.setPatientId(patientId);
        return create(dto);
    }

    @Transactional
    public void deleteByPatientId(Long patientId) {
        List<AllergyIntolerance> rows = repo.findAllByPatientId(patientId);
        if (rows.isEmpty()) return;

        String externalId = rows.get(0).getExternalId();
        repo.deleteAllByPatientId(patientId);

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                ext.delete(externalId);
            }
        }
    }

    /* ---------------------- Item-level ops ---------------------- */

    @Transactional(readOnly = true)
    public AllergyIntoleranceDto.AllergyItem getItem(Long patientId, Long intoleranceId) {
        return repo.findAllByPatientId(patientId).stream()
                .filter(r -> r.getId().equals(intoleranceId))
                .findFirst()
                .map(this::toItem)
                .orElseThrow(() -> new RuntimeException(
                        "Allergy not found for patientId=" + patientId + " intoleranceId=" + intoleranceId));
    }

    @Transactional
    public AllergyIntoleranceDto.AllergyItem updateItem(Long patientId, Long intoleranceId,
                                                        AllergyIntoleranceDto.AllergyItem patch) {
        List<AllergyIntolerance> rows = repo.findAllByPatientId(patientId);

        AllergyIntolerance row = rows.stream()
                .filter(r -> r.getId().equals(intoleranceId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Allergy not found id=" + intoleranceId));

        if (patch.getAllergyName() != null) row.setAllergyName(patch.getAllergyName());
        if (patch.getReaction() != null)    row.setReaction(patch.getReaction());
        if (patch.getSeverity() != null)    row.setSeverity(patch.getSeverity());
        if (patch.getStatus() != null)      row.setStatus(patch.getStatus());
        if (patch.getStartDate() != null)   row.setStartDate(patch.getStartDate());
        if (patch.getEndDate() != null)     row.setEndDate(patch.getEndDate());
        if (patch.getComments() != null)    row.setComments(patch.getComments()); // NEW
        // Ensure mandatory fields are present after applying the patch
        validateMandatoryFields(row);
        validateDates(row.getStartDate(), row.getEndDate());
        repo.save(row);

        if (row.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                List<AllergyIntolerance> fresh = repo.findAllByPatientId(patientId);
                ext.update(toDto(patientId, fresh, true), row.getExternalId());
            }
        }
        return toItem(row);
    }

    @Transactional
    public void deleteItem(Long patientId, Long intoleranceId) {
        List<AllergyIntolerance> rows = repo.findAllByPatientId(patientId);
        String externalId = rows.stream().findFirst().map(AllergyIntolerance::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientId(intoleranceId, patientId);
        if (n == 0) throw new RuntimeException("Delete failed: not found");

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<AllergyIntoleranceDto> ext = storageResolver.resolve(AllergyIntoleranceDto.class);
                List<AllergyIntolerance> fresh = repo.findAllByPatientId(patientId);
                if (fresh.isEmpty()) ext.delete(externalId);
                else ext.update(toDto(patientId, fresh, true), externalId);
            }
        }
    }

    /* ---------------------- Search all (by org, grouped by patient) ---------------------- */

    @Transactional(readOnly = true)
    public ApiResponse<List<AllergyIntoleranceDto>> searchAll() {

        List<AllergyIntolerance> all = repo.findAll();
        Map<Long, List<AllergyIntolerance>> byPatient =
                all.stream().collect(Collectors.groupingBy(AllergyIntolerance::getPatientId));

        List<AllergyIntoleranceDto> dtos = new ArrayList<>();
        for (var e : byPatient.entrySet()) {
            dtos.add(toDto(e.getKey(), e.getValue(), false));
        }

        return ApiResponse.<List<AllergyIntoleranceDto>>builder()
                .success(true)
                .message("Allergy Intolerances retrieved successfully")
                .data(dtos)
                .build();
    }

    /* ---------------------- Helpers ---------------------- */

    private AllergyIntoleranceDto toDto(Long patientId, List<AllergyIntolerance> rows,
                                        boolean includeTopLevelPatientId) {
        AllergyIntoleranceDto dto = new AllergyIntoleranceDto();
        if (includeTopLevelPatientId) {
            dto.setPatientId(patientId);
        }
        if (!rows.isEmpty()) {
            dto.setFhirId(rows.get(0).getFhirId());
            dto.setExternalId(rows.get(0).getFhirId()); // externalId is an alias for fhirId

            // Set audit information from the first row
            AllergyIntoleranceDto.Audit audit = new AllergyIntoleranceDto.Audit();
            if (rows.get(0).getCreatedDate() != null) {
                audit.setCreatedDate(rows.get(0).getCreatedDate().toString());
            }
            if (rows.get(0).getLastModifiedDate() != null) {
                audit.setLastModifiedDate(rows.get(0).getLastModifiedDate().toString());
            }
            dto.setAudit(audit);
        }
        dto.setAllergiesList(rows.stream().map(this::toItem).collect(Collectors.toList()));
        return dto;
    }

    private AllergyIntoleranceDto.AllergyItem toItem(AllergyIntolerance r) {
        AllergyIntoleranceDto.AllergyItem it = new AllergyIntoleranceDto.AllergyItem();
        it.setId(r.getId());
        it.setFhirId(r.getFhirId());
        it.setExternalId(r.getFhirId()); // externalId is an alias for fhirId
        it.setAllergyName(r.getAllergyName());
        it.setReaction(r.getReaction());
        it.setSeverity(r.getSeverity());
        it.setStatus(r.getStatus());
        it.setPatientId(r.getPatientId());
        it.setStartDate(r.getStartDate());
        it.setEndDate(r.getEndDate());
        it.setComments(r.getComments()); // NEW
        return it;
    }

    /** Validate only when both are ISO yyyy-MM-dd. */
    private void validateDates(String start, String end) {
        if (start == null || end == null) return;
        try {
            if (start.matches("\\d{4}-\\d{2}-\\d{2}") && end.matches("\\d{4}-\\d{2}-\\d{2}")) {
                if (LocalDate.parse(end).isBefore(LocalDate.parse(start))) {
                    throw new IllegalArgumentException("endDate cannot be before startDate");
                }
            }
        } catch (Exception ignore) {
            // tolerate non-ISO inputs since we store strings
        }
    }

    /** Ensure mandatory fields allergyName and reaction are present on incoming items. */
    private void validateMandatoryFields(AllergyIntoleranceDto.AllergyItem it) {
        if (it == null) throw new IllegalArgumentException("allergy item is required");
        if (isBlank(it.getAllergyName())) throw new IllegalArgumentException("allergyName is required");
        if (isBlank(it.getReaction()))    throw new IllegalArgumentException("reaction is required");
    }

    /** Ensure mandatory fields on entity after applying patches. */
    private void validateMandatoryFields(AllergyIntolerance r) {
        if (r == null) throw new IllegalArgumentException("allergy item is required");
        if (isBlank(r.getAllergyName())) throw new IllegalArgumentException("allergyName is required");
        if (isBlank(r.getReaction()))    throw new IllegalArgumentException("reaction is required");
    }


    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

