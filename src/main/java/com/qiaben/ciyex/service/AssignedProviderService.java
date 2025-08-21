package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.AssignedProviderDto;
import com.qiaben.ciyex.entity.AssignedProvider;
import com.qiaben.ciyex.repository.AssignedProviderRepository;
import com.qiaben.ciyex.storage.ExternalAssignedProviderStorage;
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
public class AssignedProviderService {

    private final AssignedProviderRepository repo;
    private final Optional<ExternalAssignedProviderStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AssignedProviderDto create(Long orgId, Long patientId, Long encounterId, AssignedProviderDto in) {
        AssignedProvider e = AssignedProvider.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .providerId(in.getProviderId())
                .role(in.getRole())
                .startDate(in.getStartDate())
                .endDate(in.getEndDate())
                .status(in.getStatus())
                .notes(in.getNotes())
                .build();

        final AssignedProvider saved = repo.save(e);

        external.ifPresent(ext -> {
            final AssignedProvider ref = saved;
            String extId = ext.create(mapToDto(ref));
            ref.setExternalId(extId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    public AssignedProviderDto update(Long orgId, Long patientId, Long encounterId, Long id, AssignedProviderDto in) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

        e.setProviderId(in.getProviderId());
        e.setRole(in.getRole());
        e.setStartDate(in.getStartDate());
        e.setEndDate(in.getEndDate());
        e.setStatus(in.getStatus());
        e.setNotes(in.getNotes());

        final AssignedProvider updated = repo.save(e);

        external.ifPresent(ext -> {
            final AssignedProvider ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));

        external.ifPresent(ext -> {
            if (e.getExternalId() != null) ext.delete(e.getExternalId());
        });

        repo.delete(e);
    }

    public AssignedProviderDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        AssignedProvider e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Assigned provider not found"));
        return mapToDto(e);
    }

    public List<AssignedProviderDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<AssignedProviderDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private AssignedProviderDto mapToDto(AssignedProvider e) {
        AssignedProviderDto dto = new AssignedProviderDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());

        dto.setProviderId(e.getProviderId());
        dto.setRole(e.getRole());
        dto.setStartDate(e.getStartDate());
        dto.setEndDate(e.getEndDate());
        dto.setStatus(e.getStatus());
        dto.setNotes(e.getNotes());

        AssignedProviderDto.Audit a = new AssignedProviderDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }
}
