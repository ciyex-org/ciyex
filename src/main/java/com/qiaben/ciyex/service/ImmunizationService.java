package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.entity.Immunization;
import com.qiaben.ciyex.repository.ImmunizationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImmunizationService {

    private final ImmunizationRepository repository;

    public ImmunizationService(ImmunizationRepository repository) {
        this.repository = repository;
    }

    // ---------- Patient-level ----------

    @Transactional
    public ImmunizationDto create(ImmunizationDto dto) {

        if (dto == null || dto.getImmunizations() == null || dto.getImmunizations().isEmpty()) {
            throw new IllegalArgumentException("No immunization data provided");
        }

        ImmunizationDto.ImmunizationItem item = dto.getImmunizations().get(0);
        // Validate mandatory fields
        validateMandatoryFields(item);
        Immunization entity = mapToEntity(dto.getPatientId(), item);

        // Auto-generate externalId if not provided
        if (entity.getFhirId() == null) {
            String generatedId = "IMM-" + System.currentTimeMillis();
            entity.setFhirId(generatedId);
            entity.setExternalId(generatedId);
            log.info("Auto-generated externalId: {}", generatedId);
        }

        entity = repository.save(entity);
        item.setId(entity.getId());
        item.setExternalId(entity.getExternalId());
        item.setPatientId(entity.getPatientId());

        return buildDtoFromEntity(entity);
    }

    @Transactional(readOnly = true)
    public ImmunizationDto getByPatientId(Long patientId) {

        List<Immunization> entities = repository.findByPatientId(patientId);
        return buildDtoFromEntities(patientId, entities);
    }

    @Transactional
    public ImmunizationDto updateByPatientId(Long patientId, ImmunizationDto dto) {


        if (dto.getImmunizations() == null || dto.getImmunizations().isEmpty()) {
            throw new IllegalArgumentException("No immunization data provided");
        }

        ImmunizationDto.ImmunizationItem patch = dto.getImmunizations().get(0);

        Immunization entity = repository.findOneByIdAndPatientId(patch.getId(), patientId);
        if (entity == null) {
            throw new RuntimeException("Immunization not found with id: " + patch.getId() + " for patientId: " + patientId);
        }

        applyPatch(entity, patch);
        // Ensure mandatory fields remain present after patch
        validateMandatoryFields(entity);
        repository.save(entity);
        return buildDtoFromEntity(entity);
    }


    @Transactional
    public void deleteByPatientId(Long patientId) {
        List<Immunization> entities = repository.findByPatientId(patientId);
        repository.deleteAll(entities);
    }

    // ---------- Item-level ----------

    @Transactional(readOnly = true)
    public ImmunizationDto.ImmunizationItem getItem(Long patientId, Long immunizationId) {
        Immunization entity = repository.findOneByIdAndPatientId(immunizationId, patientId);
        if (entity == null) {
            throw new RuntimeException("Immunization not found with id: " + immunizationId + " for patientId: " + patientId);
        }
        return mapToItem(entity);
    }

    @Transactional
    public ImmunizationDto.ImmunizationItem updateItem(Long patientId, Long immunizationId, ImmunizationDto.ImmunizationItem patch) {

        Immunization entity = repository.findOneByIdAndPatientId(immunizationId, patientId);
        if (entity == null) {
            throw new RuntimeException("Immunization not found with id: " + immunizationId + " for patientId: " + patientId);
        }

        applyPatch(entity, patch);
        // Validate mandatory fields after applying patch
        validateMandatoryFields(entity);
        repository.save(entity);
        return mapToItem(entity);
    }

    @Transactional
    public void deleteItem(Long patientId, Long immunizationId) {
        Immunization entity = repository.findOneByIdAndPatientId(immunizationId, patientId);
        if (entity == null) {
            throw new RuntimeException("Immunization not found with id: " + immunizationId + " for patientId: " + patientId);
        }
        repository.delete(entity);
    }

    // ---------- Search All ----------

    @Transactional(readOnly = true)
    public ApiResponse<List<ImmunizationDto>> searchAll() {

        List<Immunization> entities = repository.findAll();

        var grouped = entities.stream()
                .collect(Collectors.groupingBy(Immunization::getPatientId));

        List<ImmunizationDto> result = grouped.entrySet().stream()
                .map(entry -> buildDtoFromEntities(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return ApiResponse.<List<ImmunizationDto>>builder()
                .success(true)
                .message("Immunizations retrieved successfully")
                .data(result)
                .build();
    }

    // ---------- Helpers ----------

    private Immunization mapToEntity(Long patientId, ImmunizationDto.ImmunizationItem item) {
        // Use externalId if provided, otherwise use fhirId
        String fhirIdValue = item.getExternalId() != null ? item.getExternalId() : item.getFhirId();

        return Immunization.builder()
                .patientId(patientId)
                .fhirId(fhirIdValue)
                .externalId(fhirIdValue)
                .cvxCode(item.getCvxCode())
                .dateTimeAdministered(item.getDateTimeAdministered())
                .amountAdministered(item.getAmountAdministered())
                .expirationDate(item.getExpirationDate())
                .manufacturer(item.getManufacturer())
                .lotNumber(item.getLotNumber())
                .administratorName(item.getAdministratorName())
                .administratorTitle(item.getAdministratorTitle())
                .dateVisGiven(item.getDateVisGiven())
                .dateVisStatement(item.getDateVisStatement())
                .route(item.getRoute())
                .administrationSite(item.getAdministrationSite())
                .notes(item.getNotes())
                .informationSource(item.getInformationSource())
                .completionStatus(item.getCompletionStatus())
                .substanceRefusalReason(item.getSubstanceRefusalReason())
                .reasonCode(item.getReasonCode())
                .orderingProvider(item.getOrderingProvider())
                .build();
    }

    private ImmunizationDto.ImmunizationItem mapToItem(Immunization entity) {
        ImmunizationDto.ImmunizationItem item = new ImmunizationDto.ImmunizationItem();
        item.setId(entity.getId());
        item.setFhirId(entity.getFhirId());
        item.setExternalId(entity.getFhirId()); // externalId is an alias for fhirId
        item.setPatientId(entity.getPatientId());
        item.setCvxCode(entity.getCvxCode());
        item.setDateTimeAdministered(entity.getDateTimeAdministered());
        item.setAmountAdministered(entity.getAmountAdministered());
        item.setExpirationDate(entity.getExpirationDate());
        item.setManufacturer(entity.getManufacturer());
        item.setLotNumber(entity.getLotNumber());
        item.setAdministratorName(entity.getAdministratorName());
        item.setAdministratorTitle(entity.getAdministratorTitle());
        item.setDateVisGiven(entity.getDateVisGiven());
        item.setDateVisStatement(entity.getDateVisStatement());
        item.setRoute(entity.getRoute());
        item.setAdministrationSite(entity.getAdministrationSite());
        item.setNotes(entity.getNotes());
        item.setInformationSource(entity.getInformationSource());
        item.setCompletionStatus(entity.getCompletionStatus());
        item.setSubstanceRefusalReason(entity.getSubstanceRefusalReason());
        item.setReasonCode(entity.getReasonCode());
        item.setOrderingProvider(entity.getOrderingProvider());
        return item;
    }

    private void applyPatch(Immunization entity, ImmunizationDto.ImmunizationItem patch) {
        // Update fhirId if externalId or fhirId is provided
        String fhirIdValue = patch.getExternalId() != null ? patch.getExternalId() : patch.getFhirId();
        if (fhirIdValue != null) {
            entity.setFhirId(fhirIdValue);
            entity.setExternalId(fhirIdValue);
        }
        if (patch.getCvxCode() != null) entity.setCvxCode(patch.getCvxCode());
        if (patch.getDateTimeAdministered() != null) entity.setDateTimeAdministered(patch.getDateTimeAdministered());
        if (patch.getAmountAdministered() != null) entity.setAmountAdministered(patch.getAmountAdministered());
        if (patch.getExpirationDate() != null) entity.setExpirationDate(patch.getExpirationDate());
        if (patch.getManufacturer() != null) entity.setManufacturer(patch.getManufacturer());
        if (patch.getLotNumber() != null) entity.setLotNumber(patch.getLotNumber());
        if (patch.getAdministratorName() != null) entity.setAdministratorName(patch.getAdministratorName());
        if (patch.getAdministratorTitle() != null) entity.setAdministratorTitle(patch.getAdministratorTitle());
        if (patch.getDateVisGiven() != null) entity.setDateVisGiven(patch.getDateVisGiven());
        if (patch.getDateVisStatement() != null) entity.setDateVisStatement(patch.getDateVisStatement());
        if (patch.getRoute() != null) entity.setRoute(patch.getRoute());
        if (patch.getAdministrationSite() != null) entity.setAdministrationSite(patch.getAdministrationSite());
        if (patch.getNotes() != null) entity.setNotes(patch.getNotes());
        if (patch.getInformationSource() != null) entity.setInformationSource(patch.getInformationSource());
        if (patch.getCompletionStatus() != null) entity.setCompletionStatus(patch.getCompletionStatus());
        if (patch.getSubstanceRefusalReason() != null) entity.setSubstanceRefusalReason(patch.getSubstanceRefusalReason());
        if (patch.getReasonCode() != null) entity.setReasonCode(patch.getReasonCode());
        if (patch.getOrderingProvider() != null) entity.setOrderingProvider(patch.getOrderingProvider());
    }

    // ---- Validation helpers ----
    private void validateMandatoryFields(ImmunizationDto.ImmunizationItem item) {
        if (item == null) throw new IllegalArgumentException("immunization item is required");
        if (isBlank(item.getCvxCode())) throw new IllegalArgumentException("cvxCode is required");
        if (isBlank(item.getAmountAdministered())) throw new IllegalArgumentException("amountAdministered is required");
        if (isBlank(item.getManufacturer())) throw new IllegalArgumentException("manufacturer is required");
        if (isBlank(item.getAdministratorName())) throw new IllegalArgumentException("administratorName is required");
    }

    private void validateMandatoryFields(Immunization entity) {
        if (entity == null) throw new IllegalArgumentException("immunization is required");
        if (isBlank(entity.getCvxCode())) throw new IllegalArgumentException("cvxCode is required");
        if (isBlank(entity.getAmountAdministered())) throw new IllegalArgumentException("amountAdministered is required");
        if (isBlank(entity.getManufacturer())) throw new IllegalArgumentException("manufacturer is required");
        if (isBlank(entity.getAdministratorName())) throw new IllegalArgumentException("administratorName is required");
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private ImmunizationDto buildDtoFromEntity(Immunization entity) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setPatientId(entity.getPatientId());

        ImmunizationDto.Audit audit = new ImmunizationDto.Audit();
        if (entity.getCreatedDate() != null) {
            audit.setCreatedDate(entity.getCreatedDate().toString());
        }
        if (entity.getLastModifiedDate() != null) {
            audit.setLastModifiedDate(entity.getLastModifiedDate().toString());
        }
        dto.setAudit(audit);

        dto.setImmunizations(List.of(mapToItem(entity)));
        return dto;
    }

    private ImmunizationDto buildDtoFromEntities(Long patientId, List<Immunization> entities) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setPatientId(patientId);

        if (!entities.isEmpty()) {
            Immunization firstEntity = entities.get(0);
            ImmunizationDto.Audit audit = new ImmunizationDto.Audit();
            if (firstEntity.getCreatedDate() != null) {
                audit.setCreatedDate(firstEntity.getCreatedDate().toString());
            }
            if (firstEntity.getLastModifiedDate() != null) {
                audit.setLastModifiedDate(firstEntity.getLastModifiedDate().toString());
            }
            dto.setAudit(audit);
        }

        dto.setImmunizations(entities.stream().map(this::mapToItem).collect(Collectors.toList()));
        return dto;
    }
}
