package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MedicalProblemDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.MedicalProblem;
import com.qiaben.ciyex.repository.MedicalProblemRepository;
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
public class MedicalProblemService {

    private final MedicalProblemRepository repo;
    private final ExternalStorageResolver storageResolver;
    private final OrgIntegrationConfigProvider configProvider;

    public MedicalProblemService(MedicalProblemRepository repo,
                                 ExternalStorageResolver storageResolver,
                                 OrgIntegrationConfigProvider configProvider) {
        this.repo = repo;
        this.storageResolver = storageResolver;
        this.configProvider = configProvider;
    }

    /* -------- Top level -------- */

    @Transactional
    public MedicalProblemDto create(MedicalProblemDto dto) {
        Long orgId = requireOrg("create");
        if (dto.getPatientId() == null) throw new IllegalArgumentException("patientId is required");
        dto.setOrgId(orgId);

        String now = LocalDateTime.now().toString();
        List<MedicalProblem> rows = new ArrayList<>();

        if (dto.getProblemsList() != null) {
            for (var it : dto.getProblemsList()) {
                MedicalProblem row = MedicalProblem.builder()
                        .orgId(orgId)
                        .patientId(dto.getPatientId())
                        .title(it.getTitle())
                        .outcome(it.getOutcome())
                        .verificationStatus(it.getVerificationStatus())
                        .occurrence(it.getOccurrence())
                        .note(it.getNote())
                        .createdDate(now)
                        .lastModifiedDate(now)
                        .build();
                rows.add(repo.save(row));
            }
        }

        // external sync
        String storageType = configProvider.getStorageTypeForCurrentOrg();
        if (storageType != null && !rows.isEmpty()) {
            ExternalStorage<MedicalProblemDto> ext = storageResolver.resolve(MedicalProblemDto.class);
            MedicalProblemDto snapshot = toDto(orgId, dto.getPatientId(), rows, true);
            String externalId = ext.create(snapshot);
            for (MedicalProblem r : rows) {
                r.setExternalId(externalId);
                repo.save(r);
            }
        }
        return toDto(orgId, dto.getPatientId(), rows, true);
    }

    @Transactional(readOnly = true)
    public MedicalProblemDto getByPatientId(Long patientId) {
        Long orgId = requireOrg("getByPatientId");
        List<MedicalProblem> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        if (rows.isEmpty()) throw new RuntimeException("No medical problems found for patientId=" + patientId);

        MedicalProblemDto dto = toDto(orgId, patientId, rows, true);

        if (rows.get(0).getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<MedicalProblemDto> ext = storageResolver.resolve(MedicalProblemDto.class);
                MedicalProblemDto fromExt = ext.get(rows.get(0).getExternalId());
                if (fromExt != null && fromExt.getExternalId() != null) dto.setExternalId(fromExt.getExternalId());
            }
        }
        return dto;
    }

    @Transactional
    public MedicalProblemDto updateByPatientId(Long patientId, MedicalProblemDto dto) {
        Long orgId = requireOrg("updateByPatientId");
        repo.deleteAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        dto.setOrgId(orgId);
        dto.setPatientId(patientId);
        return create(dto); // replace strategy
    }

    @Transactional
    public void deleteByPatientId(Long patientId) {
        Long orgId = requireOrg("deleteByPatientId");
        List<MedicalProblem> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        if (rows.isEmpty()) return;

        String externalId = rows.get(0).getExternalId();
        repo.deleteAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<MedicalProblemDto> ext = storageResolver.resolve(MedicalProblemDto.class);
                ext.delete(externalId);
            }
        }
    }

    /* -------- Item level -------- */

    @Transactional(readOnly = true)
    public MedicalProblemDto.MedicalProblemItem getItem(Long patientId, Long problemId) {
        Long orgId = requireOrg("getItem");
        return repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId)).stream()
                .filter(r -> r.getId().equals(problemId))
                .findFirst()
                .map(this::toItem)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    @Transactional
    public MedicalProblemDto.MedicalProblemItem updateItem(Long patientId, Long problemId,
                                                           MedicalProblemDto.MedicalProblemItem patch) {
        Long orgId = requireOrg("updateItem");
        List<MedicalProblem> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));

        MedicalProblem row = rows.stream()
                .filter(r -> r.getId().equals(problemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (patch.getTitle() != null) row.setTitle(patch.getTitle());
        if (patch.getOutcome() != null) row.setOutcome(patch.getOutcome());
        if (patch.getVerificationStatus() != null) row.setVerificationStatus(patch.getVerificationStatus());
        if (patch.getOccurrence() != null) row.setOccurrence(patch.getOccurrence());
        if (patch.getNote() != null) row.setNote(patch.getNote());

        row.setLastModifiedDate(LocalDateTime.now().toString());
        repo.save(row);

        if (row.getExternalId() != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<MedicalProblemDto> ext = storageResolver.resolve(MedicalProblemDto.class);
                List<MedicalProblem> fresh = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
                ext.update(toDto(orgId, patientId, fresh, true), row.getExternalId());
            }
        }
        return toItem(row);
    }

    @Transactional
    public void deleteItem(Long patientId, Long problemId) {
        Long orgId = requireOrg("deleteItem");
        List<MedicalProblem> rows = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
        String externalId = rows.stream().findFirst().map(MedicalProblem::getExternalId).orElse(null);

        int n = repo.deleteOneByIdAndPatientIdAndOrgIdText(String.valueOf(problemId), String.valueOf(patientId), String.valueOf(orgId));
        if (n == 0) throw new RuntimeException("Delete failed");

        if (externalId != null) {
            String storageType = configProvider.getStorageTypeForCurrentOrg();
            if (storageType != null) {
                ExternalStorage<MedicalProblemDto> ext = storageResolver.resolve(MedicalProblemDto.class);
                List<MedicalProblem> fresh = repo.findAllByPatientIdAndOrgIdText(String.valueOf(patientId), String.valueOf(orgId));
                if (fresh.isEmpty()) ext.delete(externalId);
                else ext.update(toDto(orgId, patientId, fresh, true), externalId);
            }
        }
    }

    /* -------- Search all -------- */

    @Transactional(readOnly = true)
    public ApiResponse<List<MedicalProblemDto>> searchAll() {
        Long orgId = requireOrg("searchAll");
        List<MedicalProblem> all = repo.findByOrgIdText(String.valueOf(orgId));
        Map<Long, List<MedicalProblem>> byPatient = all.stream().collect(Collectors.groupingBy(MedicalProblem::getPatientId));

        List<MedicalProblemDto> dtos = new ArrayList<>();
        for (var e : byPatient.entrySet()) {
            dtos.add(toDto(orgId, e.getKey(), e.getValue(), true));
        }

        return ApiResponse.<List<MedicalProblemDto>>builder()
                .success(true)
                .message("Medical Problems retrieved successfully")
                .data(dtos)
                .build();
    }

    /* -------- Helpers -------- */

    private Long requireOrg(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    private MedicalProblemDto toDto(Long orgId, Long patientId, List<MedicalProblem> rows, boolean includeTopLevelPatientId) {
        MedicalProblemDto dto = new MedicalProblemDto();
        dto.setOrgId(orgId);
        if (includeTopLevelPatientId) dto.setPatientId(patientId);

        if (!rows.isEmpty()) {
            dto.setId(rows.get(0).getId());
            dto.setExternalId(rows.get(0).getExternalId());
            MedicalProblemDto.Audit a = new MedicalProblemDto.Audit();
            a.setCreatedDate(LocalDateTime.parse(rows.get(0).getCreatedDate()));
            a.setLastModifiedDate(LocalDateTime.parse(rows.get(0).getLastModifiedDate()));
            dto.setAudit(a);
        }
        dto.setProblemsList(rows.stream().map(this::toItem).collect(Collectors.toList()));
        return dto;
    }

    private MedicalProblemDto.MedicalProblemItem toItem(MedicalProblem r) {
        MedicalProblemDto.MedicalProblemItem it = new MedicalProblemDto.MedicalProblemItem();
        it.setId(r.getId());
        it.setTitle(r.getTitle());
        it.setOutcome(r.getOutcome());
        it.setVerificationStatus(r.getVerificationStatus());
        it.setOccurrence(r.getOccurrence());
        it.setNote(r.getNote());
        it.setPatientId(r.getPatientId());
        return it;
    }
}
