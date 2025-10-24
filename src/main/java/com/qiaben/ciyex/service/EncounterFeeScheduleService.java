


package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.FeeScheduleDto;
import com.qiaben.ciyex.dto.FeeScheduleDto.FeeScheduleEntryDto;
import com.qiaben.ciyex.entity.EncounterFeeSchedule;
import com.qiaben.ciyex.entity.EncounterFeeScheduleEntry;
import com.qiaben.ciyex.repository.EncounterFeeScheduleEntryRepository;
import com.qiaben.ciyex.repository.EncounterFeeScheduleRepository;
import com.qiaben.ciyex.storage.ExternalEncounterFeeScheduleStorage;
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
public class EncounterFeeScheduleService {

    private final EncounterFeeScheduleRepository scheduleRepo;
    private final EncounterFeeScheduleEntryRepository entryRepo;
    private final Optional<ExternalEncounterFeeScheduleStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ----- Schedules -----
    public FeeScheduleDto create(Long orgId, Long patientId, Long encounterId, FeeScheduleDto in) {
        EncounterFeeSchedule s = EncounterFeeSchedule.builder()
                .patientId(patientId).encounterId(encounterId)
                .name(in.getName())
                .payer(in.getPayer())
                .currency(in.getCurrency())
                .effectiveFrom(in.getEffectiveFrom())
                .effectiveTo(in.getEffectiveTo())
                .status(in.getStatus())
                .notes(in.getNotes())
                .build();

        EncounterFeeSchedule saved = scheduleRepo.save(s);

        external.ifPresent(ext -> {
            String extId = ext.create(mapScheduleToDto(saved, false));
            saved.setExternalId(extId);
            scheduleRepo.save(saved);
        });

        return mapScheduleToDto(saved, true);
    }

    public FeeScheduleDto update(Long orgId, Long patientId, Long encounterId, Long scheduleId, FeeScheduleDto in) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));

        // optionally verify scope:
        if (!s.getPatientId().equals(patientId) || !s.getEncounterId().equals(encounterId))
            throw new IllegalArgumentException("Schedule not in this encounter scope");

        s.setName(in.getName());
        s.setPayer(in.getPayer());
        s.setCurrency(in.getCurrency());
        s.setEffectiveFrom(in.getEffectiveFrom());
        s.setEffectiveTo(in.getEffectiveTo());
        s.setStatus(in.getStatus());
        s.setNotes(in.getNotes());

        EncounterFeeSchedule updated = scheduleRepo.save(s);

        external.ifPresent(ext -> {
            if (updated.getExternalId() != null)
                ext.update(updated.getExternalId(), mapScheduleToDto(updated, false));
        });

        return mapScheduleToDto(updated, true);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long scheduleId) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));

        if (!s.getPatientId().equals(patientId) || !s.getEncounterId().equals(encounterId))
            throw new IllegalArgumentException("Schedule not in this encounter scope");

        external.ifPresent(ext -> { if (s.getExternalId() != null) ext.delete(s.getExternalId()); });
        scheduleRepo.delete(s);
    }

    public FeeScheduleDto getOne(Long orgId, Long patientId, Long encounterId, Long scheduleId) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));
        if (!s.getPatientId().equals(patientId) || !s.getEncounterId().equals(encounterId))
            throw new IllegalArgumentException("Schedule not in this encounter scope");
        return mapScheduleToDto(s, true);
    }

    public List<FeeScheduleDto> listInEncounter(Long orgId, Long patientId, Long encounterId) {
        return scheduleRepo.findByOrgIdAndPatientIdAndEncounterId(orgId, patientId, encounterId)
                .stream().map(s -> mapScheduleToDto(s, false)).toList();
    }

    public List<FeeScheduleDto> listByPatient(Long orgId, Long patientId) {
        return scheduleRepo.findByOrgIdAndPatientId(orgId, patientId).stream()
                .map(s -> mapScheduleToDto(s, false)).toList();
    }

    // ----- Entries -----
    public FeeScheduleEntryDto addEntry(Long orgId, Long patientId, Long encounterId, Long scheduleId, FeeScheduleEntryDto in) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));
        verifyScope(s, orgId, patientId, encounterId);

        EncounterFeeScheduleEntry e = EncounterFeeScheduleEntry.builder()
                .schedule(s)
                .codeType(in.getCodeType()).code(in.getCode()).modifier(in.getModifier())
                .description(in.getDescription()).unit(in.getUnit())
                .currency(in.getCurrency()).amount(in.getAmount())
                .active(in.getActive()).notes(in.getNotes())
                .build();

        return mapEntryToDto(entryRepo.save(e));
    }

    public FeeScheduleEntryDto updateEntry(Long orgId, Long patientId, Long encounterId, Long scheduleId, Long entryId, FeeScheduleEntryDto in) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));
        verifyScope(s, orgId, patientId, encounterId);

        EncounterFeeScheduleEntry e = entryRepo.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));
        if (!e.getSchedule().getId().equals(s.getId()))
            throw new IllegalArgumentException("Entry does not belong to this schedule");

        e.setCodeType(in.getCodeType()); e.setCode(in.getCode()); e.setModifier(in.getModifier());
        e.setDescription(in.getDescription()); e.setUnit(in.getUnit());
        e.setCurrency(in.getCurrency()); e.setAmount(in.getAmount());
        e.setActive(in.getActive()); e.setNotes(in.getNotes());

        return mapEntryToDto(entryRepo.save(e));
    }

    public void deleteEntry(Long orgId, Long patientId, Long encounterId, Long scheduleId, Long entryId) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));
        verifyScope(s, orgId, patientId, encounterId);

        EncounterFeeScheduleEntry e = entryRepo.findById(entryId)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found"));
        if (!e.getSchedule().getId().equals(s.getId()))
            throw new IllegalArgumentException("Entry does not belong to this schedule");

        entryRepo.delete(e);
    }

    public List<FeeScheduleEntryDto> listEntries(Long orgId, Long patientId, Long encounterId, Long scheduleId) {
        EncounterFeeSchedule s = scheduleRepo.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Fee schedule not found"));
        verifyScope(s, orgId, patientId, encounterId);
        return entryRepo.findBySchedule(s).stream().map(this::mapEntryToDto).toList();
    }

    public List<FeeScheduleEntryDto> searchEntries(Long orgId, Long patientId, Long encounterId,
                                                   Long scheduleId, String codeType, Boolean active, String q) {
        // scheduleId not strictly required for search; scope is org/patient/encounter
        return entryRepo.search(orgId, patientId, encounterId, codeType, active, q).stream()
                .map(this::mapEntryToDto).toList();
    }

    // ----- Mapping helpers -----
    private FeeScheduleDto mapScheduleToDto(EncounterFeeSchedule s, boolean includeEntries) {
        FeeScheduleDto dto = new FeeScheduleDto();
        dto.setId(s.getId()); dto.setExternalId(s.getExternalId());
        dto.setPatientId(s.getPatientId()); dto.setEncounterId(s.getEncounterId());
        dto.setName(s.getName()); dto.setPayer(s.getPayer()); dto.setCurrency(s.getCurrency());
        dto.setEffectiveFrom(s.getEffectiveFrom()); dto.setEffectiveTo(s.getEffectiveTo());
        dto.setStatus(s.getStatus()); dto.setNotes(s.getNotes());

        if (includeEntries && s.getEntries() != null)
            dto.setEntries(s.getEntries().stream().map(this::mapEntryToDto).toList());

        FeeScheduleDto.Audit a = new FeeScheduleDto.Audit();
        if (s.getCreatedAt() != null) a.setCreatedDate(DTF.format(s.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (s.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(s.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);
        return dto;
    }

    private FeeScheduleDto.FeeScheduleEntryDto mapEntryToDto(EncounterFeeScheduleEntry e) {
        FeeScheduleDto.FeeScheduleEntryDto dto = new FeeScheduleDto.FeeScheduleEntryDto();
        dto.setId(e.getId()); dto.setScheduleId(e.getSchedule().getId());
        dto.setCodeType(e.getCodeType()); dto.setCode(e.getCode()); dto.setModifier(e.getModifier());
        dto.setDescription(e.getDescription()); dto.setUnit(e.getUnit());
        dto.setCurrency(e.getCurrency()); dto.setAmount(e.getAmount());
        dto.setActive(e.getActive()); dto.setNotes(e.getNotes());
        return dto;
    }

    private void verifyScope(EncounterFeeSchedule s, Long orgId, Long patientId, Long encounterId) {
        if ( !s.getPatientId().equals(patientId) || !s.getEncounterId().equals(encounterId))
            throw new IllegalArgumentException("Resource not in this encounter scope");
    }
}
