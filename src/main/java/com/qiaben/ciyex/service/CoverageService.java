package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.CoverageDto;
import com.qiaben.ciyex.dto.InsuranceCompanyDto;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.qiaben.ciyex.entity.Coverage;
import com.qiaben.ciyex.entity.InsuranceCompany;
import com.qiaben.ciyex.repository.CoverageRepository;
import com.qiaben.ciyex.repository.InsuranceCompanyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CoverageService {

    private final CoverageRepository coverageRepository;
    private final InsuranceCompanyRepository insuranceCompanyRepository;

    public CoverageService(CoverageRepository coverageRepository,
                           InsuranceCompanyRepository insuranceCompanyRepository) {
        this.coverageRepository = coverageRepository;
        this.insuranceCompanyRepository = insuranceCompanyRepository;
    }

    // ---- CRUD (create stays same) ----

    @Transactional
    public CoverageDto create(CoverageDto dto) {
        Long orgId = getCurrentOrgIdOrThrow("create");
        dto.setOrgId(orgId); // enforce header value

        // If your design maps orgId => insurance company row id, keep this:
        InsuranceCompany insuranceCompany = insuranceCompanyRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Insurance company not found for orgId: " + orgId));

        Coverage coverage = mapToEntity(dto);
        coverage.setInsuranceCompany(insuranceCompany);
        Coverage saved = coverageRepository.save(coverage);

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public CoverageDto getById(Long id) {
        Long orgId = getCurrentOrgIdOrThrow("getById");
        Coverage coverage = coverageRepository
                .findByIdAndOrgIdText(String.valueOf(id), String.valueOf(orgId))
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));
        return mapToDto(coverage);
    }

    @Transactional
    public CoverageDto update(Long id, CoverageDto dto) {
        Long orgId = getCurrentOrgIdOrThrow("update");
        Coverage coverage = coverageRepository
                .findByIdAndOrgIdText(String.valueOf(id), String.valueOf(orgId))
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));

        dto.setOrgId(orgId);
        updateEntityFromDto(coverage, dto);
        Coverage saved = coverageRepository.save(coverage);
        return mapToDto(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long orgId = getCurrentOrgIdOrThrow("delete");
        Coverage coverage = coverageRepository
                .findByIdAndOrgIdText(String.valueOf(id), String.valueOf(orgId))
                .orElseThrow(() -> new RuntimeException("Coverage not found with id: " + id));
        coverageRepository.delete(coverage);
    }

    @Transactional(readOnly = true)
    public List<CoverageDto> getAllCoverages() {
        Long orgId = getCurrentOrgIdOrThrow("getAllCoverages");
        List<Coverage> coverages = coverageRepository.findAllByOrgIdText(String.valueOf(orgId));
        return coverages.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // ---- Composite (id + patientId) ops using text-cast queries ----

    @Transactional(readOnly = true)
    public CoverageDto getByIdAndPatientId(Long id, Long patientId) {
        Long orgId = getCurrentOrgIdOrThrow("getByIdAndPatientId");
        Coverage coverage = coverageRepository
                .findByIdAndPatientIdAndOrgIdText(String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId))
                .orElseThrow(() ->
                        new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));
        return mapToDto(coverage);
    }

    @Transactional
    public CoverageDto updateByIdAndPatientId(Long id, Long patientId, CoverageDto dto) {
        Long orgId = getCurrentOrgIdOrThrow("updateByIdAndPatientId");
        Coverage coverage = coverageRepository
                .findByIdAndPatientIdAndOrgIdText(String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId))
                .orElseThrow(() ->
                        new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));

        dto.setOrgId(orgId);
        updateEntityFromDto(coverage, dto);
        Coverage saved = coverageRepository.save(coverage);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteByIdAndPatientId(Long id, Long patientId) {
        Long orgId = getCurrentOrgIdOrThrow("deleteByIdAndPatientId");
        Coverage coverage = coverageRepository
                .findByIdAndPatientIdAndOrgIdText(String.valueOf(id), String.valueOf(patientId), String.valueOf(orgId))
                .orElseThrow(() ->
                        new RuntimeException("Coverage not found for id=" + id + ", patientId=" + patientId));
        coverageRepository.delete(coverage);
    }

    // ---- helpers ----

    private Long getCurrentOrgIdOrThrow(String op) {
        Long orgId = RequestContext.get() != null ? RequestContext.get().getOrgId() : null;
        if (orgId == null) throw new SecurityException("No orgId in RequestContext during " + op);
        return orgId;
    }

    private Coverage mapToEntity(CoverageDto dto) {
        return Coverage.builder()
                .externalId(dto.getExternalId())
                .coverageType(dto.getCoverageType())
                .planName(dto.getPlanName())
                .policyNumber(dto.getPolicyNumber())
                .coverageStartDate(dto.getCoverageStartDate())
                .coverageEndDate(dto.getCoverageEndDate())
                .patientId(dto.getPatientId())
                .orgId(dto.getOrgId())
                .provider(dto.getProvider())
                .effectiveDate(dto.getEffectiveDate())
                .effectiveDateEnd(dto.getEffectiveDateEnd())
                .groupNumber(dto.getGroupNumber())
                .subscriberEmployer(dto.getSubscriberEmployer())
                .subscriberAddressLine1(dto.getSubscriberAddressLine1())
                .subscriberAddressLine2(dto.getSubscriberAddressLine2())
                .subscriberCity(dto.getSubscriberCity())
                .subscriberState(dto.getSubscriberState())
                .subscriberZipCode(dto.getSubscriberZipCode())
                .subscriberCountry(dto.getSubscriberCountry())
                .subscriberPhone(dto.getSubscriberPhone())
                .byholderName(dto.getByholderName())
                .byholderRelation(dto.getByholderRelation())
                .byholderAddressLine1(dto.getByholderAddressLine1())
                .byholderAddressLine2(dto.getByholderAddressLine2())
                .byholderCity(dto.getByholderCity())
                .byholderState(dto.getByholderState())
                .byholderZipCode(dto.getByholderZipCode())
                .byholderCountry(dto.getByholderCountry())
                .byholderPhone(dto.getByholderPhone())
                .copayAmount(dto.getCopayAmount())
                .build();
    }

    private CoverageDto mapToDto(Coverage coverage) {
        CoverageDto dto = new CoverageDto();
        dto.setId(coverage.getId());
        dto.setExternalId(coverage.getExternalId());
        dto.setCoverageType(coverage.getCoverageType());
        dto.setPlanName(coverage.getPlanName());
        dto.setPolicyNumber(coverage.getPolicyNumber());
        dto.setCoverageStartDate(coverage.getCoverageStartDate());
        dto.setCoverageEndDate(coverage.getCoverageEndDate());
        dto.setPatientId(coverage.getPatientId());
        dto.setOrgId(coverage.getOrgId());
        dto.setProvider(coverage.getProvider());
        dto.setEffectiveDate(coverage.getEffectiveDate());
        dto.setEffectiveDateEnd(coverage.getEffectiveDateEnd());
        dto.setGroupNumber(coverage.getGroupNumber());
        dto.setSubscriberEmployer(coverage.getSubscriberEmployer());
        dto.setSubscriberAddressLine1(coverage.getSubscriberAddressLine1());
        dto.setSubscriberAddressLine2(coverage.getSubscriberAddressLine2());
        dto.setSubscriberCity(coverage.getSubscriberCity());
        dto.setSubscriberState(coverage.getSubscriberState());
        dto.setSubscriberZipCode(coverage.getSubscriberZipCode());
        dto.setSubscriberCountry(coverage.getSubscriberCountry());
        dto.setSubscriberPhone(coverage.getSubscriberPhone());
        dto.setByholderName(coverage.getByholderName());
        dto.setByholderRelation(coverage.getByholderRelation());
        dto.setByholderAddressLine1(coverage.getByholderAddressLine1());
        dto.setByholderAddressLine2(coverage.getByholderAddressLine2());
        dto.setByholderCity(coverage.getByholderCity());
        dto.setByholderState(coverage.getByholderState());
        dto.setByholderZipCode(coverage.getByholderZipCode());
        dto.setByholderCountry(coverage.getByholderCountry());
        dto.setByholderPhone(coverage.getByholderPhone());
        dto.setCopayAmount(coverage.getCopayAmount());

        if (coverage.getInsuranceCompany() != null) {
            InsuranceCompany ic = coverage.getInsuranceCompany();
            InsuranceCompanyDto insuranceCompanyDto = new InsuranceCompanyDto();
            insuranceCompanyDto.setId(ic.getId());
            insuranceCompanyDto.setName(ic.getName());
            insuranceCompanyDto.setAddress(ic.getAddress());
            insuranceCompanyDto.setCity(ic.getCity());
            insuranceCompanyDto.setState(ic.getState());
            insuranceCompanyDto.setPostalCode(ic.getPostalCode());
            insuranceCompanyDto.setCountry(ic.getCountry());
            insuranceCompanyDto.setFhirId(ic.getFhirId());
            InsuranceCompanyDto.Audit audit = new InsuranceCompanyDto.Audit();
            audit.setCreatedDate(ic.getCreatedDate());
            audit.setLastModifiedDate(ic.getLastModifiedDate());
            insuranceCompanyDto.setAudit(audit);
            dto.setInsuranceCompany(insuranceCompanyDto);
        }
        return dto;
    }

    private void updateEntityFromDto(Coverage coverage, CoverageDto dto) {
        if (dto.getCoverageType() != null) coverage.setCoverageType(dto.getCoverageType());
        if (dto.getPlanName() != null) coverage.setPlanName(dto.getPlanName());
        if (dto.getPolicyNumber() != null) coverage.setPolicyNumber(dto.getPolicyNumber());
        if (dto.getCoverageStartDate() != null) coverage.setCoverageStartDate(dto.getCoverageStartDate());
        if (dto.getCoverageEndDate() != null) coverage.setCoverageEndDate(dto.getCoverageEndDate());
        if (dto.getPatientId() != null) coverage.setPatientId(dto.getPatientId());
        if (dto.getOrgId() != null) coverage.setOrgId(dto.getOrgId());
        if (dto.getProvider() != null) coverage.setProvider(dto.getProvider());
        if (dto.getEffectiveDate() != null) coverage.setEffectiveDate(dto.getEffectiveDate());
        if (dto.getEffectiveDateEnd() != null) coverage.setEffectiveDateEnd(dto.getEffectiveDateEnd());
        if (dto.getGroupNumber() != null) coverage.setGroupNumber(dto.getGroupNumber());
        if (dto.getSubscriberEmployer() != null) coverage.setSubscriberEmployer(dto.getSubscriberEmployer());
        if (dto.getSubscriberAddressLine1() != null) coverage.setSubscriberAddressLine1(dto.getSubscriberAddressLine1());
        if (dto.getSubscriberAddressLine2() != null) coverage.setSubscriberAddressLine2(dto.getSubscriberAddressLine2());
        if (dto.getSubscriberCity() != null) coverage.setSubscriberCity(dto.getSubscriberCity());
        if (dto.getSubscriberState() != null) coverage.setSubscriberState(dto.getSubscriberState());
        if (dto.getSubscriberZipCode() != null) coverage.setSubscriberZipCode(dto.getSubscriberZipCode());
        if (dto.getSubscriberCountry() != null) coverage.setSubscriberCountry(dto.getSubscriberCountry());
        if (dto.getSubscriberPhone() != null) coverage.setSubscriberPhone(dto.getSubscriberPhone());
        if (dto.getByholderName() != null) coverage.setByholderName(dto.getByholderName());
        if (dto.getByholderRelation() != null) coverage.setByholderRelation(dto.getByholderRelation());
        if (dto.getByholderAddressLine1() != null) coverage.setByholderAddressLine1(dto.getByholderAddressLine1());
        if (dto.getByholderAddressLine2() != null) coverage.setByholderAddressLine2(dto.getByholderAddressLine2());
        if (dto.getByholderCity() != null) coverage.setByholderCity(dto.getByholderCity());
        if (dto.getByholderState() != null) coverage.setByholderState(dto.getByholderState());
        if (dto.getByholderZipCode() != null) coverage.setByholderZipCode(dto.getByholderZipCode());
        if (dto.getByholderCountry() != null) coverage.setByholderCountry(dto.getByholderCountry());
        if (dto.getByholderPhone() != null) coverage.setByholderPhone(dto.getByholderPhone());
        if (dto.getCopayAmount() != null) coverage.setCopayAmount(dto.getCopayAmount());
    }
}
