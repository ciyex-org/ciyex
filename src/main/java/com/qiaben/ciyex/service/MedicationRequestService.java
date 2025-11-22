package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.MedicationRequestDto;
import com.qiaben.ciyex.entity.MedicationRequest;
import com.qiaben.ciyex.repository.MedicationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicationRequestService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MedicationRequestRepository repository;

    @Autowired
    public MedicationRequestService(MedicationRequestRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MedicationRequestDto create(MedicationRequestDto dto) {
        // Validate mandatory fields
        validateMandatoryFields(dto);
        
        MedicationRequest entity = mapToEntity(dto);
        String currentDate = LocalDateTime.now().format(DATE_FORMATTER);

        MedicationRequest savedEntity = repository.save(entity);
        return mapToDto(savedEntity);
    }
    
    private void validateMandatoryFields(MedicationRequestDto dto) {
        StringBuilder errors = new StringBuilder();
        
        if (dto.getPatientId() == null) {
            errors.append("patientId, ");
        }
        if (dto.getEncounterId() == null) {
            errors.append("encounterId, ");
        }
        if (dto.getMedicationName() == null || dto.getMedicationName().trim().isEmpty()) {
            errors.append("medicationName, ");
        }
        
        if (errors.length() > 0) {
            // Remove trailing comma and space
            String missingFields = errors.substring(0, errors.length() - 2);
            throw new IllegalArgumentException("Missing mandatory fields: " + missingFields);
        }
    }

    @Transactional(readOnly = true)
    public MedicationRequestDto getById(Long id) {
        MedicationRequest entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MedicationRequest not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public MedicationRequestDto update(Long id, MedicationRequestDto dto) {
        MedicationRequest entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MedicationRequest not found with id: " + id));
        entity = updateEntityFromDto(entity, dto);
        MedicationRequest updatedEntity = repository.save(entity);
        return mapToDto(updatedEntity);
    }

    @Transactional
    public void delete(Long id) {
        MedicationRequest entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("MedicationRequest not found with id: " + id));
        repository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<MedicationRequestDto> getAllByPatientIdOrEncounterId(Long patientId, Long encounterId) {
        List<MedicationRequest> entities;
        if (patientId != null && encounterId != null) {
            entities = repository.findByPatientIdOrEncounterId(patientId, encounterId);
        } else if (patientId != null) {
            entities = repository.findByPatientId(patientId);
        } else if (encounterId != null) {
            entities = repository.findByEncounterId(encounterId);
        } else {
            entities = repository.findAll();
        }
        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private MedicationRequestDto mapToDto(MedicationRequest entity) {
        MedicationRequestDto dto = new MedicationRequestDto();
        dto.setId(entity.getId());
        dto.setPatientId(entity.getPatientId());
        dto.setEncounterId(entity.getEncounterId());
        dto.setMedicationName(entity.getMedicationName());
        dto.setDosage(entity.getDosage());
        dto.setInstructions(entity.getInstructions());
        dto.setDateIssued(entity.getDateIssued());
        dto.setPrescribingDoctor(entity.getPrescribingDoctor());
        dto.setStatus(entity.getStatus());

        // Initialize and set audit dates
        MedicationRequestDto.Audit audit = new MedicationRequestDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().format(DATE_FORMATTER));
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().format(DATE_FORMATTER));
        }
        dto.setAudit(audit);

        return dto;
    }

    private MedicationRequest mapToEntity(MedicationRequestDto dto) {
        MedicationRequest entity = new MedicationRequest();
        entity.setPatientId(dto.getPatientId());
        entity.setEncounterId(dto.getEncounterId());
        entity.setMedicationName(dto.getMedicationName());
        entity.setDosage(dto.getDosage());
        entity.setInstructions(dto.getInstructions());
        entity.setDateIssued(dto.getDateIssued());
        entity.setPrescribingDoctor(dto.getPrescribingDoctor());
        entity.setStatus(dto.getStatus());
        return entity;
    }

    private MedicationRequest updateEntityFromDto(MedicationRequest entity, MedicationRequestDto dto) {
        if (dto.getMedicationName() != null) entity.setMedicationName(dto.getMedicationName());
        if (dto.getDosage() != null) entity.setDosage(dto.getDosage());
        if (dto.getInstructions() != null) entity.setInstructions(dto.getInstructions());
        if (dto.getDateIssued() != null) entity.setDateIssued(dto.getDateIssued());
        if (dto.getPrescribingDoctor() != null) entity.setPrescribingDoctor(dto.getPrescribingDoctor());
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getEncounterId() != null) entity.setEncounterId(dto.getEncounterId());
        return entity;
    }
}