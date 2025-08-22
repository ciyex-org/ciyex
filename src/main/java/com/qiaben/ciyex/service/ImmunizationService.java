package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.entity.Immunization;
import com.qiaben.ciyex.repository.ImmunizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ImmunizationService {

    private final ImmunizationRepository repository;

    @Autowired
    public ImmunizationService(ImmunizationRepository repository) {
        this.repository = repository;
    }

    // Create Immunization
    public ImmunizationDto create(ImmunizationDto dto, Long orgId) {
        Immunization entity = mapToEntity(dto);
        entity.setOrgId(orgId);
        Immunization savedEntity = repository.save(entity);
        return mapToDto(savedEntity);
    }

    // Read All Immunizations by orgId
    public List<ImmunizationDto> getByOrgId(Long orgId) {
        List<Immunization> immunizations = repository.findByOrgId(orgId);
        return immunizations.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Read Immunization by id
    public ImmunizationDto getById(Long id) {
        Optional<Immunization> immunization = repository.findById(id);
        return immunization.map(this::mapToDto).orElse(null);
    }

    // Update Immunization by id
    public ImmunizationDto update(Long id, ImmunizationDto dto, Long orgId) {
        Optional<Immunization> immunizationOptional = repository.findById(id);
        if (immunizationOptional.isPresent()) {
            Immunization existingImmunization = immunizationOptional.get();
            existingImmunization = mapToEntity(dto);
            existingImmunization.setId(id);
            existingImmunization.setOrgId(orgId);
            Immunization updatedEntity = repository.save(existingImmunization);
            return mapToDto(updatedEntity);
        } else {
            return null; // Immunization not found
        }
    }

    // Delete Immunization by id
    public boolean delete(Long id) {
        Optional<Immunization> immunization = repository.findById(id);
        if (immunization.isPresent()) {
            repository.delete(immunization.get());
            return true;
        }
        return false;
    }

    private ImmunizationDto mapToDto(Immunization entity) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setId(entity.getId());
        dto.setVaccine(entity.getVaccine());
        dto.setDose(entity.getDose());
        dto.setDateAdministered(entity.getDateAdministered());
        dto.setAmountAdministered(entity.getAmountAdministered());
        dto.setImmunizationExpirationDate(entity.getImmunizationExpirationDate());
        dto.setImmunizationManufacturer(entity.getImmunizationManufacturer());
        dto.setImmunizationLotNumber(entity.getImmunizationLotNumber());
        dto.setAdministratorName(entity.getAdministratorName());
        dto.setDateInformationGiven(entity.getDateInformationGiven());
        dto.setDateVISStatement(entity.getDateVISStatement());
        dto.setRoute(entity.getRoute());
        dto.setAdministrationSite(entity.getAdministrationSite());
        dto.setNotes(entity.getNotes());
        dto.setInformationSource(entity.getInformationSource());
        dto.setCompletionStatus(entity.getCompletionStatus());
        dto.setSubstanceRefusalReason(entity.getSubstanceRefusalReason());
        dto.setReasonCode(entity.getReasonCode());
        dto.setImmunizationOrderingProvider(entity.getImmunizationOrderingProvider());
        dto.setPatientId(entity.getPatientId());
        dto.setOrgId(entity.getOrgId());
        return dto;
    }

    private Immunization mapToEntity(ImmunizationDto dto) {
        Immunization entity = new Immunization();
        entity.setVaccine(dto.getVaccine());
        entity.setDose(dto.getDose());
        entity.setDateAdministered(dto.getDateAdministered());
        entity.setAmountAdministered(dto.getAmountAdministered());
        entity.setImmunizationExpirationDate(dto.getImmunizationExpirationDate());
        entity.setImmunizationManufacturer(dto.getImmunizationManufacturer());
        entity.setImmunizationLotNumber(dto.getImmunizationLotNumber());
        entity.setAdministratorName(dto.getAdministratorName());
        entity.setDateInformationGiven(dto.getDateInformationGiven());
        entity.setDateVISStatement(dto.getDateVISStatement());
        entity.setRoute(dto.getRoute());
        entity.setAdministrationSite(dto.getAdministrationSite());
        entity.setNotes(dto.getNotes());
        entity.setInformationSource(dto.getInformationSource());
        entity.setCompletionStatus(dto.getCompletionStatus());
        entity.setSubstanceRefusalReason(dto.getSubstanceRefusalReason());
        entity.setReasonCode(dto.getReasonCode());
        entity.setImmunizationOrderingProvider(dto.getImmunizationOrderingProvider());
        entity.setPatientId(dto.getPatientId());
        return entity;
    }
}
