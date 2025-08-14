package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientMedicalHistoryDto;
import com.qiaben.ciyex.entity.PatientMedicalHistory;
import com.qiaben.ciyex.repository.PatientMedicalHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PatientMedicalHistoryService {

    private final PatientMedicalHistoryRepository repository;

    public PatientMedicalHistoryService(PatientMedicalHistoryRepository repository) {
        this.repository = repository;
    }

    public PatientMedicalHistoryDto createMedicalHistory(Long orgId, PatientMedicalHistoryDto dto) {
        // You can use orgId for validation or other operations if required
        PatientMedicalHistory entity = PatientMedicalHistory.builder()
                .patientId(dto.getPatientId())
                .medicalCondition(dto.getMedicalCondition())
                .diagnosisDetails(dto.getDiagnosisDetails())
                .diagnosisDate(dto.getDiagnosisDate())
                .treatmentDetails(dto.getTreatmentDetails())
                .isChronic(dto.getIsChronic())
                .build();

        PatientMedicalHistory savedEntity = repository.save(entity);
        return mapToDto(savedEntity);
    }

    public List<PatientMedicalHistoryDto> getPatientMedicalHistory(Long orgId, Long patientId) {
        // You can use orgId for filtering data specific to the organization if required
        List<PatientMedicalHistory> histories = repository.findByPatientId(patientId);
        return histories.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private PatientMedicalHistoryDto mapToDto(PatientMedicalHistory entity) {
        return PatientMedicalHistoryDto.builder()
                .patientId(entity.getPatientId())
                .medicalCondition(entity.getMedicalCondition())
                .diagnosisDetails(entity.getDiagnosisDetails())
                .diagnosisDate(entity.getDiagnosisDate())
                .treatmentDetails(entity.getTreatmentDetails())
                .isChronic(entity.getIsChronic())
                .build();
    }
}
