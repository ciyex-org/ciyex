package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.ApiResponse;
import com.qiaben.ciyex.dto.FacilityDto;
import com.qiaben.ciyex.entity.Facility;
import com.qiaben.ciyex.repository.FacilityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FacilityService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FacilityRepository repository;

    public FacilityService(FacilityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FacilityDto create(FacilityDto dto) {
        log.info("Creating new facility: {}", dto.getName());

        Facility entity = mapToEntity(dto);
        Facility saved = repository.save(entity);

        log.info("Facility created successfully with id: {}", saved.getId());
        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public FacilityDto getById(Long id) {
        log.info("Fetching facility by id: {}", id);

        Facility facility = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facility not found with id: " + id));

        return mapToDto(facility);
    }

    @Transactional
    public FacilityDto update(Long id, FacilityDto dto) {
        log.info("Updating facility with id: {}", id);

        Facility existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facility not found with id: " + id));

        updateEntityFromDto(existing, dto);
        Facility updated = repository.save(existing);

        log.info("Facility updated successfully: {}", id);
        return mapToDto(updated);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting facility with id: {}", id);

        Facility facility = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facility not found with id: " + id));

        repository.delete(facility);
        log.info("Facility deleted successfully: {}", id);
    }

    @Transactional
    public void softDelete(Long id) {
        log.info("Soft deleting facility with id: {}", id);

        Facility facility = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facility not found with id: " + id));

        facility.setIsActive(false);
        facility.setFacilityInactive(true);
        repository.save(facility);

        log.info("Facility soft deleted successfully: {}", id);
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<FacilityDto>> getAllFacilities() {
        log.info("Fetching all facilities");

        List<Facility> facilities = repository.findAll();
        List<FacilityDto> dtos = facilities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<FacilityDto>> getActiveFacilities() {
        log.info("Fetching active facilities");

        List<Facility> facilities = repository.findAllByIsActiveTrue();
        List<FacilityDto> dtos = facilities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Active facilities retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<FacilityDto>> getFacilitiesByStatus(Boolean isActive) {
        log.info("Fetching facilities by status: {}", isActive);

        List<Facility> facilities = repository.findAllByIsActive(isActive);
        List<FacilityDto> dtos = facilities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities retrieved successfully")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<FacilityDto>> searchByName(String name) {
        log.info("Searching facilities by name: {}", name);

        List<Facility> facilities = repository.findByNameContainingIgnoreCase(name);
        List<FacilityDto> dtos = facilities.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        return ApiResponse.<List<FacilityDto>>builder()
                .success(true)
                .message("Facilities search completed")
                .data(dtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<FacilityDto> getAllPaginated(Pageable pageable) {
        log.info("Fetching paginated facilities");

        Page<Facility> facilities = repository.findAll(pageable);
        return facilities.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public long getTotalCount() {
        return repository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveCount() {
        return repository.findAllByIsActiveTrue().size();
    }

    @Transactional(readOnly = true)
    public long getInactiveCount() {
        return repository.findAllByIsActive(false).size();
    }

    private Facility mapToEntity(FacilityDto dto) {
        return Facility.builder()
                .name(dto.getName())
                .physicalAddress(dto.getPhysicalAddress())
                .physicalCity(dto.getPhysicalCity())
                .physicalState(dto.getPhysicalState())
                .physicalZipCode(dto.getPhysicalZipCode())
                .physicalCountry(dto.getPhysicalCountry())
                .mailingAddress(dto.getMailingAddress())
                .mailingCity(dto.getMailingCity())
                .mailingState(dto.getMailingState())
                .mailingZipCode(dto.getMailingZipCode())
                .mailingCountry(dto.getMailingCountry())
                .phone(dto.getPhone())
                .fax(dto.getFax())
                .website(dto.getWebsite())
                .email(dto.getEmail())
                .color(dto.getColor())
                .iban(dto.getIban())
                .posCode(dto.getPosCode())
                .facilityTaxonomy(dto.getFacilityTaxonomy())
                .cliaNumber(dto.getCliaNumber())
                .taxIdType(dto.getTaxIdType())
                .taxId(dto.getTaxId())
                .billingAttn(dto.getBillingAttn())
                .facilityLabCode(dto.getFacilityLabCode())
                .npi(dto.getNpi())
                .oid(dto.getOid())
                .billingLocation(dto.getBillingLocation())
                .acceptsAssignment(dto.getAcceptsAssignment())
                .serviceLocation(dto.getServiceLocation())
                .primaryBusinessEntity(dto.getPrimaryBusinessEntity())
                .facilityInactive(dto.getFacilityInactive())
                .info(dto.getInfo())
                .isActive(dto.getIsActive() != null ? dto.getIsActive() : true)
                .externalId(dto.getExternalId())
                .build();
    }

    private void updateEntityFromDto(Facility entity, FacilityDto dto) {
        entity.setName(dto.getName());
        entity.setPhysicalAddress(dto.getPhysicalAddress());
        entity.setPhysicalCity(dto.getPhysicalCity());
        entity.setPhysicalState(dto.getPhysicalState());
        entity.setPhysicalZipCode(dto.getPhysicalZipCode());
        entity.setPhysicalCountry(dto.getPhysicalCountry());
        entity.setMailingAddress(dto.getMailingAddress());
        entity.setMailingCity(dto.getMailingCity());
        entity.setMailingState(dto.getMailingState());
        entity.setMailingZipCode(dto.getMailingZipCode());
        entity.setMailingCountry(dto.getMailingCountry());
        entity.setPhone(dto.getPhone());
        entity.setFax(dto.getFax());
        entity.setWebsite(dto.getWebsite());
        entity.setEmail(dto.getEmail());
        entity.setColor(dto.getColor());
        entity.setIban(dto.getIban());
        entity.setPosCode(dto.getPosCode());
        entity.setFacilityTaxonomy(dto.getFacilityTaxonomy());
        entity.setCliaNumber(dto.getCliaNumber());
        entity.setTaxIdType(dto.getTaxIdType());
        entity.setTaxId(dto.getTaxId());
        entity.setBillingAttn(dto.getBillingAttn());
        entity.setFacilityLabCode(dto.getFacilityLabCode());
        entity.setNpi(dto.getNpi());
        entity.setOid(dto.getOid());
        entity.setBillingLocation(dto.getBillingLocation());
        entity.setAcceptsAssignment(dto.getAcceptsAssignment());
        entity.setServiceLocation(dto.getServiceLocation());
        entity.setPrimaryBusinessEntity(dto.getPrimaryBusinessEntity());
        entity.setFacilityInactive(dto.getFacilityInactive());
        entity.setInfo(dto.getInfo());
        entity.setIsActive(dto.getIsActive());
        entity.setExternalId(dto.getExternalId());
    }

    private FacilityDto mapToDto(Facility entity) {
        FacilityDto.Audit audit = FacilityDto.Audit.builder()
                .createdDate(entity.getCreatedDate() != null ? entity.getCreatedDate().format(DATE_FORMATTER) : null)
                .lastModifiedDate(entity.getLastModifiedDate() != null ? entity.getLastModifiedDate().format(DATE_FORMATTER) : null)
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .build();

        return FacilityDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .physicalAddress(entity.getPhysicalAddress())
                .physicalCity(entity.getPhysicalCity())
                .physicalState(entity.getPhysicalState())
                .physicalZipCode(entity.getPhysicalZipCode())
                .physicalCountry(entity.getPhysicalCountry())
                .mailingAddress(entity.getMailingAddress())
                .mailingCity(entity.getMailingCity())
                .mailingState(entity.getMailingState())
                .mailingZipCode(entity.getMailingZipCode())
                .mailingCountry(entity.getMailingCountry())
                .phone(entity.getPhone())
                .fax(entity.getFax())
                .website(entity.getWebsite())
                .email(entity.getEmail())
                .color(entity.getColor())
                .iban(entity.getIban())
                .posCode(entity.getPosCode())
                .facilityTaxonomy(entity.getFacilityTaxonomy())
                .cliaNumber(entity.getCliaNumber())
                .taxIdType(entity.getTaxIdType())
                .taxId(entity.getTaxId())
                .billingAttn(entity.getBillingAttn())
                .facilityLabCode(entity.getFacilityLabCode())
                .npi(entity.getNpi())
                .oid(entity.getOid())
                .billingLocation(entity.getBillingLocation())
                .acceptsAssignment(entity.getAcceptsAssignment())
                .serviceLocation(entity.getServiceLocation())
                .primaryBusinessEntity(entity.getPrimaryBusinessEntity())
                .facilityInactive(entity.getFacilityInactive())
                .info(entity.getInfo())
                .isActive(entity.getIsActive())
                .externalId(entity.getExternalId())
                .audit(audit)
                .build();
    }
}