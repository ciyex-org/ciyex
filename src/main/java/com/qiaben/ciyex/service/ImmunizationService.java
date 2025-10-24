package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.ImmunizationDto;
import com.qiaben.ciyex.entity.Immunization;
import com.qiaben.ciyex.repository.ImmunizationRepository;
import com.qiaben.ciyex.dto.integration.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        Long orgId = getCurrentOrgId();

        ImmunizationDto.ImmunizationItem item = dto.getImmunizations().get(0);
        Immunization entity = mapToEntity(dto.getPatientId(), orgId, item);
        entity.setCreatedDate(LocalDateTime.now().toString());
        entity.setLastModifiedDate(LocalDateTime.now().toString());

        entity = repository.save(entity);
        item.setId(entity.getId());
        item.setExternalId(entity.getExternalId());
        item.setPatientId(entity.getPatientId());

        return buildDtoFromEntity(entity);
    }

    @Transactional(readOnly = true)
    public ImmunizationDto getByPatientId(Long patientId) {
        Long orgId = getCurrentOrgId();
        List<Immunization> entities = repository.findByPatientIdAndOrgId(patientId, orgId);
        return buildDtoFromEntities(patientId, orgId, entities);
    }

    @Transactional
    public ImmunizationDto updateByPatientId(Long patientId, ImmunizationDto dto) {
        Long orgId = getCurrentOrgId();

        if (dto.getImmunizations() == null || dto.getImmunizations().isEmpty()) {
            throw new IllegalArgumentException("No immunization data provided");
        }

        ImmunizationDto.ImmunizationItem patch = dto.getImmunizations().get(0);

        Immunization entity = repository.findOneByIdAndPatientIdAndOrgId(
                        patch.getId(), patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Immunization not found"));

        applyPatch(entity, patch);
        entity.setLastModifiedDate(LocalDateTime.now().toString());

        repository.save(entity);
        return buildDtoFromEntity(entity);
    }


    @Transactional
    public void deleteByPatientId(Long patientId) {
        Long orgId = getCurrentOrgId();
        List<Immunization> entities = repository.findByPatientIdAndOrgId(patientId, orgId);
        repository.deleteAll(entities);
    }

    // ---------- Item-level ----------

    @Transactional(readOnly = true)
    public ImmunizationDto.ImmunizationItem getItem(Long patientId, Long immunizationId) {
        Long orgId = getCurrentOrgId();
        Immunization entity = repository.findOneByIdAndPatientIdAndOrgId(immunizationId, patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        return mapToItem(entity);
    }

    @Transactional
    public ImmunizationDto.ImmunizationItem updateItem(Long patientId, Long immunizationId, ImmunizationDto.ImmunizationItem patch) {
        Long orgId = getCurrentOrgId();
        Immunization entity = repository.findOneByIdAndPatientIdAndOrgId(immunizationId, patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Not found"));

        applyPatch(entity, patch);
        entity.setLastModifiedDate(LocalDateTime.now().toString());
        repository.save(entity);
        return mapToItem(entity);
    }

    @Transactional
    public void deleteItem(Long patientId, Long immunizationId) {
        Long orgId = getCurrentOrgId();
        Immunization entity = repository.findOneByIdAndPatientIdAndOrgId(immunizationId, patientId, orgId)
                .orElseThrow(() -> new RuntimeException("Not found"));
        repository.delete(entity);
    }

    // ---------- Search All ----------

    @Transactional(readOnly = true)
    public ApiResponse<List<ImmunizationDto>> searchAll() {
        Long orgId = getCurrentOrgId();
        List<Immunization> entities = repository.findAllByOrgId(orgId);

        var grouped = entities.stream()
                .collect(Collectors.groupingBy(Immunization::getPatientId));

        List<ImmunizationDto> result = grouped.entrySet().stream()
                .map(entry -> buildDtoFromEntities(entry.getKey(), orgId, entry.getValue()))
                .collect(Collectors.toList());

        return ApiResponse.<List<ImmunizationDto>>builder()
                .success(true)
                .message("Immunizations retrieved successfully")
                .data(result)
                .build();
    }

    // ---------- Helpers ----------

    private Immunization mapToEntity(Long patientId, Long orgId, ImmunizationDto.ImmunizationItem item) {
        return Immunization.builder()
                .patientId(patientId)
                .externalId(item.getExternalId())
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
        item.setExternalId(entity.getExternalId());
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
        if (patch.getExternalId() != null) entity.setExternalId(patch.getExternalId());
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

    private ImmunizationDto buildDtoFromEntity(Immunization entity) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setPatientId(entity.getPatientId());

        ImmunizationDto.Audit audit = new ImmunizationDto.Audit();
        audit.setCreatedDate(entity.getCreatedDate());
        audit.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setAudit(audit);

        dto.setImmunizations(List.of(mapToItem(entity)));
        return dto;
    }

    private ImmunizationDto buildDtoFromEntities(Long patientId, Long orgId, List<Immunization> entities) {
        ImmunizationDto dto = new ImmunizationDto();
        dto.setPatientId(patientId);

        if (!entities.isEmpty()) {
            Immunization latest = entities.get(entities.size() - 1);
            ImmunizationDto.Audit audit = new ImmunizationDto.Audit();
            audit.setCreatedDate(latest.getCreatedDate());
            audit.setLastModifiedDate(latest.getLastModifiedDate());
            dto.setAudit(audit);
        }

        dto.setImmunizations(entities.stream().map(this::mapToItem).collect(Collectors.toList()));
        return dto;
    }

    private Long getCurrentOrgId() {
        return RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
    }
}
