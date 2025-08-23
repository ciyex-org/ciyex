package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ReviewOfSystemDto;
import com.qiaben.ciyex.entity.ReviewOfSystem;
import com.qiaben.ciyex.repository.ReviewOfSystemRepository;
import com.qiaben.ciyex.storage.ExternalReviewOfSystemStorage;
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
public class ReviewOfSystemService {

    private final ReviewOfSystemRepository repo;
    private final Optional<ExternalReviewOfSystemStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // CREATE
    public ReviewOfSystemDto create(Long orgId, Long patientId, Long encounterId, ReviewOfSystemDto in) {
        ReviewOfSystem e = ReviewOfSystem.builder()
                .orgId(orgId).patientId(patientId).encounterId(encounterId)
                .systemName(in.getSystemName())
                .isNegative(in.getIsNegative())
                .notes(in.getNotes())
                .systemDetails(in.getSystemDetails() == null ? List.of() : in.getSystemDetails())
                .build();

        final ReviewOfSystem saved = repo.save(e);

        external.ifPresent(ext -> {
            final ReviewOfSystem ref = saved;
            String extId = ext.create(mapToDto(ref));
            ref.setExternalId(extId);
            repo.save(ref);
        });

        return mapToDto(saved);
    }

    // UPDATE
    public ReviewOfSystemDto update(Long orgId, Long patientId, Long encounterId, Long id, ReviewOfSystemDto in) {
        ReviewOfSystem e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));

        e.setSystemName(in.getSystemName());
        e.setIsNegative(in.getIsNegative());
        e.setNotes(in.getNotes());
        e.setSystemDetails(in.getSystemDetails() == null ? List.of() : in.getSystemDetails());

        final ReviewOfSystem updated = repo.save(e);

        external.ifPresent(ext -> {
            final ReviewOfSystem ref = updated;
            if (ref.getExternalId() != null) ext.update(ref.getExternalId(), mapToDto(ref));
        });

        return mapToDto(updated);
    }

    // DELETE
    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        ReviewOfSystem e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));

        external.ifPresent(ext -> { if (e.getExternalId() != null) ext.delete(e.getExternalId()); });
        repo.delete(e);
    }

    // GET ONE
    public ReviewOfSystemDto getOne(Long orgId, Long patientId, Long encounterId, Long id) {
        ReviewOfSystem e = repo.findByOrgIdAndPatientIdAndEncounterIdAndId(orgId, patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("ROS not found"));
        return mapToDto(e);
    }

    // LISTS
    public List<ReviewOfSystemDto> getAllByPatient(Long orgId, Long patientId) {
        return repo.findByOrgIdAndPatientId(orgId, patientId).stream().map(this::mapToDto).toList();
    }

    public List<ReviewOfSystemDto> getAllByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    // MAPPING
    private ReviewOfSystemDto mapToDto(ReviewOfSystem e) {
        ReviewOfSystemDto dto = new ReviewOfSystemDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setOrgId(e.getOrgId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());
        dto.setSystemName(e.getSystemName());
        dto.setIsNegative(e.getIsNegative());
        dto.setNotes(e.getNotes());
        dto.setSystemDetails(e.getSystemDetails());

        if (e.getCreatedAt() != null)
            dto.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null)
            dto.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));

        return dto;
    }
}
