package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.PatientRelationshipDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.PatientRelationship;
import com.qiaben.ciyex.repository.PatientRelationshipRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PatientRelationshipService {

    private final PatientRelationshipRepository repository;

    @Autowired
    public PatientRelationshipService(PatientRelationshipRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PatientRelationshipDto create(PatientRelationshipDto dto) {
        Long orgId = getCurrentOrgId();

        PatientRelationship entity = PatientRelationship.builder()
                .orgId(orgId) // THIS WAS MISSING - CAUSING THE ERROR
                .patientId(dto.getPatientId())
                .relatedPatientId(dto.getRelatedPatientId())
                .relatedPatientName(dto.getRelatedPatientName())
                .relationshipType(dto.getRelationshipType())
                .phoneNumber(dto.getPhoneNumber())
                .email(dto.getEmail())
                .address(dto.getAddress())
                .emergencyContact(dto.getEmergencyContact() != null ? dto.getEmergencyContact() : false)
                .notes(dto.getNotes())
                .active(dto.getActive() != null ? dto.getActive() : true)
                .createdDate(LocalDateTime.now())
                .lastModifiedDate(LocalDateTime.now())
                .build();

        PatientRelationship saved = repository.save(entity);
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<PatientRelationshipDto> getAllByPatientId(Long patientId) {
        Long orgId = getCurrentOrgId();
        List<PatientRelationship> relationships = repository.findByOrgIdAndPatientId(orgId, patientId);
        return relationships.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientRelationshipDto getById(Long id) {
        Long orgId = getCurrentOrgId();
        PatientRelationship entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Patient relationship not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public PatientRelationshipDto update(Long id, PatientRelationshipDto dto) {
        Long orgId = getCurrentOrgId();
        PatientRelationship entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Patient relationship not found with id: " + id));

        entity.setRelatedPatientId(dto.getRelatedPatientId());
        entity.setRelatedPatientName(dto.getRelatedPatientName());
        entity.setRelationshipType(dto.getRelationshipType());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setEmergencyContact(dto.getEmergencyContact());
        entity.setNotes(dto.getNotes());
        entity.setActive(dto.getActive());
        entity.setLastModifiedDate(LocalDateTime.now());

        PatientRelationship updated = repository.save(entity);
        return mapToDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgId();
        PatientRelationship entity = repository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Patient relationship not found with id: " + id));
        repository.delete(entity);
    }

    private PatientRelationshipDto mapToDto(PatientRelationship entity) {
        return PatientRelationshipDto.builder()
                .id(entity.getId())
                .orgId(entity.getOrgId())
                .patientId(entity.getPatientId())
                .relatedPatientId(entity.getRelatedPatientId())
                .relatedPatientName(entity.getRelatedPatientName())
                .relationshipType(entity.getRelationshipType())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .address(entity.getAddress())
                .emergencyContact(entity.getEmergencyContact())
                .notes(entity.getNotes())
                .active(entity.getActive())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    private Long getCurrentOrgId() {
        // For development, return a default orgId
        // In production, this should come from security context
        return 1L;
    }
}