




package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ProcedureDto;
import com.qiaben.ciyex.entity.Procedure;
import com.qiaben.ciyex.repository.ProcedureRepository;
import com.qiaben.ciyex.storage.ExternalProcedureStorage;
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
public class ProcedureService {
    // Removed duplicate getAllByPatient(Long) method

    private final ProcedureRepository repo;
    private final Optional<ExternalProcedureStorage> external;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ProcedureDto create(Long patientId, Long encounterId, ProcedureDto in) {
    Procedure p = Procedure.builder()
        .patientId(patientId)
        .encounterId(encounterId)
        .cpt4(in.getCpt4())
        .description(in.getDescription())
        .units(in.getUnits())
        .rate(in.getRate())
        .relatedIcds(in.getRelatedIcds())
        .hospitalBillingStart(in.getHospitalBillingStart())
        .hospitalBillingEnd(in.getHospitalBillingEnd())
        .modifier1(in.getModifier1())
        .modifier2(in.getModifier2())
        .modifier3(in.getModifier3())
        .modifier4(in.getModifier4())
        .note(in.getNote())
        .priceLevelTitle(in.getPriceLevelTitle())
        .priceLevelId(in.getPriceLevelId())
        .providername(in.getProvidername())
        .build();

        final Procedure saved = repo.save(p);

        external.ifPresent(ext -> {
            final Procedure ref = saved;
            String externalId = ext.create(mapToDto(ref));
            ref.setExternalId(externalId);
            repo.save(ref);
        });



        return mapToDto(saved);
    }

    public ProcedureDto update(Long patientId, Long encounterId, Long id, ProcedureDto in) {
        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Procedure not found"));

        p.setCpt4(in.getCpt4());
        p.setDescription(in.getDescription());
        p.setUnits(in.getUnits());
        p.setRate(in.getRate());
        p.setRelatedIcds(in.getRelatedIcds());
        p.setHospitalBillingStart(in.getHospitalBillingStart());
        p.setHospitalBillingEnd(in.getHospitalBillingEnd());
        p.setModifier1(in.getModifier1());
        p.setModifier2(in.getModifier2());
        p.setModifier3(in.getModifier3());
        p.setModifier4(in.getModifier4());
        p.setNote(in.getNote());
        p.setPriceLevelTitle(in.getPriceLevelTitle());
        p.setProvidername(in.getProvidername());
        final Procedure updated = repo.save(p);

        external.ifPresent(ext -> {
            final Procedure ref = updated;
            if (ref.getExternalId() != null) {
                ext.update(ref.getExternalId(), mapToDto(ref));
            }
        });

        return mapToDto(updated);
    }

    public void delete(Long patientId, Long encounterId, Long id) {
        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Procedure not found"));

        final Procedure toDelete = p;
        external.ifPresent(ext -> {
            if (toDelete.getExternalId() != null) {
                ext.delete(toDelete.getExternalId());
            }
        });

        repo.delete(toDelete);
    }

    public ProcedureDto getOne(Long patientId, Long encounterId, Long id) {
        Procedure p = repo.findByPatientIdAndEncounterIdAndId(patientId, encounterId, id)
                .orElseThrow(() -> new IllegalArgumentException("Procedure not found"));
        return mapToDto(p);
    }

    public List<ProcedureDto> getAllByPatient(Long patientId) {
        return repo.findByPatientId(patientId).stream().map(this::mapToDto).toList();
    }

    public List<ProcedureDto> getAllByEncounter(Long patientId, Long encounterId) {
        return repo.findByPatientIdAndEncounterId(patientId, encounterId).stream().map(this::mapToDto).toList();
    }

    private ProcedureDto mapToDto(Procedure e) {
        ProcedureDto dto = new ProcedureDto();
        dto.setId(e.getId());
        dto.setExternalId(e.getExternalId());
        dto.setPatientId(e.getPatientId());
        dto.setEncounterId(e.getEncounterId());

        dto.setCpt4(e.getCpt4());
        dto.setDescription(e.getDescription());
        dto.setUnits(e.getUnits());
        dto.setRate(e.getRate());
        dto.setRelatedIcds(e.getRelatedIcds());
        dto.setHospitalBillingStart(e.getHospitalBillingStart());
        dto.setHospitalBillingEnd(e.getHospitalBillingEnd());
        dto.setModifier1(e.getModifier1());
        dto.setModifier2(e.getModifier2());
        dto.setModifier3(e.getModifier3());
        dto.setModifier4(e.getModifier4());
        dto.setNote(e.getNote());
        dto.setPriceLevelTitle(e.getPriceLevelTitle());
        dto.setProvidername(e.getProvidername());
        ProcedureDto.Audit a = new ProcedureDto.Audit();
        if (e.getCreatedAt() != null) a.setCreatedDate(DTF.format(e.getCreatedAt().atZone(ZoneId.systemDefault())));
        if (e.getUpdatedAt() != null) a.setLastModifiedDate(DTF.format(e.getUpdatedAt().atZone(ZoneId.systemDefault())));
        dto.setAudit(a);

        return dto;
    }
}
