package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.MedicalProblemDto;
import com.qiaben.ciyex.entity.MedicalProblem;
import com.qiaben.ciyex.repository.MedicalProblemRepository;
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

    public MedicalProblemService(MedicalProblemRepository repo) {
        this.repo = repo;
    }

    /* -------- Create -------- */

    @Transactional
    public MedicalProblemDto create(MedicalProblemDto dto) {
        if (dto.getPatientId() == null)
            throw new IllegalArgumentException("patientId is required");

        List<MedicalProblem> rows = new ArrayList<>();

        if (dto.getProblemsList() != null) {
            for (var it : dto.getProblemsList()) {
                MedicalProblem row = MedicalProblem.builder()
                        .patientId(dto.getPatientId())
                        .title(it.getTitle())
                        .outcome(it.getOutcome())
                        .verificationStatus(it.getVerificationStatus())
                        .occurrence(it.getOccurrence())
                        .note(it.getNote())
                        .build();
                rows.add(repo.save(row));
            }
        }

        return toDto(dto.getPatientId(), rows, true);
    }

    /* -------- Get by Patient -------- */

    @Transactional(readOnly = true)
    public MedicalProblemDto getByPatientId(Long patientId) {
        List<MedicalProblem> rows = repo.findAllByPatientId(patientId);
        if (rows.isEmpty())
            throw new RuntimeException("No medical problems found for patientId=" + patientId);

        return toDto(patientId, rows, true);
    }

    /* -------- Update by Patient -------- */

    @Transactional
    public MedicalProblemDto updateByPatientId(Long patientId, MedicalProblemDto dto) {
        repo.deleteAllByPatientId(patientId);
        dto.setPatientId(patientId);
        return create(dto); // replace strategy
    }

    /* -------- Delete by Patient -------- */

    @Transactional
    public void deleteByPatientId(Long patientId) {
        repo.deleteAllByPatientId(patientId);
    }

    /* -------- Single Item Operations -------- */

    @Transactional(readOnly = true)
    public MedicalProblemDto.MedicalProblemItem getItem(Long patientId, Long problemId) {
        return repo.findAllByPatientId(patientId).stream()
                .filter(r -> r.getId().equals(problemId))
                .findFirst()
                .map(this::toItem)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    @Transactional
    public MedicalProblemDto.MedicalProblemItem updateItem(
            Long patientId, Long problemId, MedicalProblemDto.MedicalProblemItem patch) {

        List<MedicalProblem> rows = repo.findAllByPatientId(patientId);

        MedicalProblem row = rows.stream()
                .filter(r -> r.getId().equals(problemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not found"));

        if (patch.getTitle() != null) row.setTitle(patch.getTitle());
        if (patch.getOutcome() != null) row.setOutcome(patch.getOutcome());
        if (patch.getVerificationStatus() != null) row.setVerificationStatus(patch.getVerificationStatus());
        if (patch.getOccurrence() != null) row.setOccurrence(patch.getOccurrence());
        if (patch.getNote() != null) row.setNote(patch.getNote());

        repo.save(row);
        return toItem(row);
    }

    @Transactional
    public void deleteItem(Long patientId, Long problemId) {
        int n = repo.deleteByIdAndPatientId(problemId, patientId);
        if (n == 0)
            throw new RuntimeException("Delete failed — record not found");
    }

    /* -------- Search All -------- */

    @Transactional(readOnly = true)
    public ApiResponse<List<MedicalProblemDto>> searchAll() {
        List<MedicalProblem> all = repo.findAll();
        Map<Long, List<MedicalProblem>> byPatient = all.stream()
                .collect(Collectors.groupingBy(MedicalProblem::getPatientId));

        List<MedicalProblemDto> dtos = new ArrayList<>();
        for (var e : byPatient.entrySet()) {
            dtos.add(toDto(e.getKey(), e.getValue(), true));
        }

        return ApiResponse.<List<MedicalProblemDto>>builder()
                .success(true)
                .message("Medical Problems retrieved successfully")
                .data(dtos)
                .build();
    }

    /* -------- Helpers -------- */

    private MedicalProblemDto toDto(Long patientId, List<MedicalProblem> rows, boolean includeTopLevelPatientId) {
        MedicalProblemDto dto = new MedicalProblemDto();
        if (includeTopLevelPatientId) dto.setPatientId(patientId);

        if (!rows.isEmpty()) {
            dto.setId(rows.get(0).getId());
            MedicalProblemDto.Audit audit = new MedicalProblemDto.Audit();
            dto.setAudit(audit);
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
