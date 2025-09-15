package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.VitalsDto;

import com.qiaben.ciyex.entity.Vitals;
import com.qiaben.ciyex.repository.VitalsRepository;
import com.qiaben.ciyex.storage.ExternalVitalsStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VitalsService {
    private final VitalsRepository repository;
    private final ExternalVitalsStorage externalStorage;

    public VitalsDto create(Long orgId, Long patientId, Long encounterId, VitalsDto dto) {
        Vitals entity = toEntity(dto);
        entity.setOrgId(orgId);
        entity.setPatientId(patientId);
        entity.setEncounterId(encounterId);
        Vitals saved = repository.save(entity);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public VitalsDto get(Long orgId, Long patientId, Long encounterId, Long id) {
        return repository.findById(id)
                .filter(v -> v.getOrgId().equals(orgId)
                        && v.getPatientId().equals(patientId)
                        && v.getEncounterId().equals(encounterId))
                .map(this::toDto)
                .orElse(null);
    }

    public List<VitalsDto> getByEncounter(Long orgId, Long patientId, Long encounterId) {
        return repository.findByPatientIdAndEncounterId(patientId, encounterId).stream()
                .filter(v -> v.getOrgId().equals(orgId))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public VitalsDto update(Long orgId, Long patientId, Long encounterId, Long id, VitalsDto dto) {
        Vitals existing = repository.findById(id).orElseThrow();
        if (!existing.getOrgId().equals(orgId)
                || !existing.getPatientId().equals(patientId)
                || !existing.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Vitals does not belong to org/patient/encounter");
        }
        existing.setWeightKg(dto.getWeightKg());
        existing.setBpSystolic(dto.getBpSystolic());
        existing.setBpDiastolic(dto.getBpDiastolic());
        existing.setPulse(dto.getPulse());
        existing.setRespiration(dto.getRespiration());
        existing.setTemperatureC(dto.getTemperatureC());
        existing.setOxygenSaturation(dto.getOxygenSaturation());
        existing.setNotes(dto.getNotes());
        Vitals updated = repository.save(existing);
        externalStorage.save(updated);
        return toDto(updated);
    }

    public void delete(Long orgId, Long patientId, Long encounterId, Long id) {
        Vitals existing = repository.findById(id).orElseThrow();
        if (existing.getOrgId().equals(orgId)
                && existing.getPatientId().equals(patientId)
                && existing.getEncounterId().equals(encounterId)) {
            repository.delete(existing);
            externalStorage.delete(id);
        }
    }

    public VitalsDto eSign(Long orgId, Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id).orElseThrow();
        if (!vitals.getOrgId().equals(orgId)
                || !vitals.getPatientId().equals(patientId)
                || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Vitals does not belong to org/patient/encounter");
        }
        vitals.setSigned(true);
        Vitals saved = repository.save(vitals);
        externalStorage.save(saved);
        return toDto(saved);
    }

    public byte[] print(Long orgId, Long patientId, Long encounterId, Long id) {
        Vitals vitals = repository.findById(id).orElseThrow();
        if (!vitals.getOrgId().equals(orgId)
                || !vitals.getPatientId().equals(patientId)
                || !vitals.getEncounterId().equals(encounterId)) {
            throw new IllegalArgumentException("Vitals does not belong to org/patient/encounter");
        }
        return externalStorage.print(vitals);
    }

    private Vitals toEntity(VitalsDto dto) {
        return Vitals.builder()
                .id(dto.getId())
                .orgId(dto.getOrgId())
                .patientId(dto.getPatientId())
                .encounterId(dto.getEncounterId())
                .weightKg(dto.getWeightKg())
                .bpSystolic(dto.getBpSystolic())
                .bpDiastolic(dto.getBpDiastolic())
                .pulse(dto.getPulse())
                .respiration(dto.getRespiration())
                .temperatureC(dto.getTemperatureC())
                .oxygenSaturation(dto.getOxygenSaturation())
                .notes(dto.getNotes())
                .signed(dto.getSigned())
                .recordedAt(dto.getRecordedAt())
                .createdDate(dto.getCreatedDate())
                .lastModifiedDate(dto.getLastModifiedDate())
                .build();
    }

    private VitalsDto toDto(Vitals entity) {
        return VitalsDto.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .patientId(entity.getPatientId())
                .encounterId(entity.getEncounterId())
                .weightKg(entity.getWeightKg())
                .bpSystolic(entity.getBpSystolic())
                .bpDiastolic(entity.getBpDiastolic())
                .pulse(entity.getPulse())
                .respiration(entity.getRespiration())
                .temperatureC(entity.getTemperatureC())
                .oxygenSaturation(entity.getOxygenSaturation())
                .notes(entity.getNotes())
                .signed(entity.getSigned())
                .recordedAt(entity.getRecordedAt())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
