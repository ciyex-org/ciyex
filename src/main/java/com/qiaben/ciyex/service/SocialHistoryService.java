package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.SocialHistoryDto;
import com.qiaben.ciyex.entity.SocialHistory;
import com.qiaben.ciyex.entity.SocialHistoryEntry;
import com.qiaben.ciyex.repository.SocialHistoryRepository;
import com.qiaben.ciyex.storage.ExternalSocialHistoryStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialHistoryService {

    private final SocialHistoryRepository repo;
    private final Optional<ExternalSocialHistoryStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SocialHistoryDto create(Long orgId, Long patientId, Long encounterId, SocialHistoryDto in) {
        SocialHistory sh = new SocialHistory();
        sh.setOrgId(orgId);
        sh.setPatientId(patientId);
        sh.setEncounterId(encounterId);

        if (in.getEntries() != null) {
            for (var e : in.getEntries()) {
                SocialHistoryEntry row = SocialHistoryEntry.builder()
                        .category(normalizeCategory(e.getCategory()))
                        .value(e.getValue())
                        .details(e.getDetails())
                        .socialHistory(sh)
                        .build();
                sh.getEntries().add(row);
            }
        }

        final SocialHistory saved = repo.save(sh);

        external.ifPresent(ext -> {
            final SocialHistory ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    public SocialHistoryDto update(Long orgId, Long patientId, Long encounterId, Long id, SocialHistoryDto in) {
        SocialHistory sh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));

        sh.getEntries().clear();
        if (in.getEntries() != null) {
            for (var e : in.getEntries()) {
                SocialHistoryEntry row = SocialHistoryEntry.builder()
                        .category(normalizeCategory(e.getCategory()))
                        .value(e.getValue())
                        .details(e.getDetails())
                        .socialHistory(sh)
                        .build();
                sh.getEntries().add(row);
            }
        }

        final SocialHistory updated = repo.save(sh);

        external.ifPresent(ext -> {
            final SocialHistory ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        SocialHistory sh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));

        final SocialHistory toDelete = sh;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    public SocialHistoryDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        SocialHistory sh = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Social History not found"));
        return mapToDto(sh);
    }

    public List<SocialHistoryDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<SocialHistoryDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private SocialHistoryDto mapToDto(SocialHistory sh) {
        SocialHistoryDto dto = new SocialHistoryDto();
        dto.setId(sh.getId());
        dto.setExternalId(sh.getExternalId());
        dto.setOrgId(sh.getOrgId());
        dto.setPatientId(sh.getPatientId());
        dto.setEncounterId(sh.getEncounterId());

        dto.setEntries(sh.getEntries().stream().map(e -> {
            SocialHistoryDto.EntryDto ed = new SocialHistoryDto.EntryDto();
            ed.setCategory(e.getCategory());
            ed.setValue(e.getValue());
            ed.setDetails(e.getDetails());
            return ed;
        }).toList());

        SocialHistoryDto.Audit a = new SocialHistoryDto.Audit();
        if (sh.getCreatedAt() != null) a.setCreatedDate(sh.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        if (sh.getUpdatedAt() != null) a.setLastModifiedDate(sh.getUpdatedAt().atZone(ZoneId.systemDefault()).toLocalDate().toString());
        dto.setAudit(a);

        return dto;
    }

    private String normalizeCategory(String c) {
        if (c == null) return "OTHER";
        String v = c.trim().toUpperCase().replace(' ', '_');
        return switch (v) {
            case "SMOKING", "ALCOHOL", "DRUGS", "OCCUPATION", "MARITAL_STATUS",
                 "EXERCISE", "DIET", "HOUSING", "EDUCATION", "SEXUAL_HISTORY" -> v;
            default -> "OTHER";
        };
    }
}
