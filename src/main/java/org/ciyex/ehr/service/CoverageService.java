package org.ciyex.ehr.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.gclient.ReferenceClientParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.ciyex.ehr.dto.ApiResponse;
import org.ciyex.ehr.dto.CoverageDto;
import org.ciyex.ehr.dto.InsuranceCompanyDto;
import org.ciyex.ehr.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CoverageService {

    private final FhirClientService fhirClientService;
    private final PracticeContextService practiceContextService;
    private final InsuranceCompanyService insuranceCompanyService;

    @Autowired
    public CoverageService(FhirClientService fhirClientService, PracticeContextService practiceContextService, InsuranceCompanyService insuranceCompanyService) {
        this.fhirClientService = fhirClientService;
        this.practiceContextService = practiceContextService;
        this.insuranceCompanyService = insuranceCompanyService;
    }

    private String getPracticeId() {
        return practiceContextService.getPracticeId();
    }

    public CoverageDto create(CoverageDto dto) {
        if (dto.getPatientId() == null) throw new IllegalArgumentException("patientId is required");
        log.info("Creating coverage in FHIR for patient: {}", dto.getPatientId());

        Coverage fhirCoverage = toFhirCoverage(dto);
        MethodOutcome outcome = fhirClientService.create(fhirCoverage, getPracticeId());
        String fhirId = outcome.getId().getIdPart();

        dto.setFhirId(fhirId);
        dto.setExternalId(fhirId);
        
        try {
            dto.setId(Long.valueOf(fhirId));
        } catch (NumberFormatException e) {
            dto.setId((long) fhirId.hashCode());
        }
        
        CoverageDto.Audit audit = new CoverageDto.Audit();
        String currentTime = java.time.Instant.now().toString();
        audit.setCreatedDate(currentTime);
        audit.setLastModifiedDate(currentTime);
        dto.setAudit(audit);
        
        log.info("Created FHIR Coverage with ID: {}", fhirId);
        
        // Load the created coverage to get the complete data including insurance company
        try {
            Coverage createdCoverage = fhirClientService.read(Coverage.class, fhirId, getPracticeId());
            return fromFhirCoverage(createdCoverage);
        } catch (Exception e) {
            log.warn("Could not reload created coverage, returning original DTO: {}", e.getMessage());
            return dto;
        }
    }

    public CoverageDto getByPatientId(Long patientId) {
        log.debug("Getting FHIR Coverages for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Coverage.class)
                .where(new ReferenceClientParam("beneficiary").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        List<CoverageDto> coverages = extractCoverages(bundle);
        
        if (!coverages.isEmpty()) {
            return coverages.get(0);
        }
        
        return new CoverageDto();
    }

    public CoverageDto updateByPatientId(Long patientId, CoverageDto dto) {
        log.info("Updating coverage in FHIR for patient: {}", patientId);
        deleteByPatientId(patientId);
        dto.setPatientId(patientId);
        return create(dto);
    }

    public void deleteByPatientId(Long patientId) {
        log.info("Deleting all FHIR Coverages for patient: {}", patientId);

        Bundle bundle = fhirClientService.getClient(getPracticeId()).search()
                .forResource(Coverage.class)
                .where(new ReferenceClientParam("beneficiary").hasId("Patient/" + patientId))
                .returnBundle(Bundle.class)
                .execute();

        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Coverage) {
                    String fhirId = entry.getResource().getIdElement().getIdPart();
                    fhirClientService.delete(Coverage.class, fhirId, getPracticeId());
                    log.debug("Deleted FHIR Coverage: {}", fhirId);
                }
            }
        }
    }

    public CoverageDto getItem(Long patientId, Long coverageId) {
        String fhirId = String.valueOf(coverageId);
        try {
            Coverage fhirCoverage = fhirClientService.read(Coverage.class, fhirId, getPracticeId());
            return fromFhirCoverage(fhirCoverage);
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Coverage not found for patientId=" + patientId + " coverageId=" + coverageId);
        }
    }

    public CoverageDto updateItem(Long patientId, Long coverageId, CoverageDto patch) {
        String fhirId = String.valueOf(coverageId);
        log.info("Updating FHIR Coverage with ID: {}", fhirId);

        // Check if resource exists first
        try {
            fhirClientService.read(Coverage.class, fhirId, getPracticeId());
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Coverage not found for patientId=" + patientId + " coverageId=" + coverageId);
        }

        Coverage fhirCoverage = toFhirCoverage(patch);
        patch.setPatientId(patientId);
        fhirCoverage.setId(fhirId);

        fhirClientService.update(fhirCoverage, getPracticeId());
        
        // Load the updated coverage to get complete data including insurance company and audit info
        try {
            Coverage updatedCoverage = fhirClientService.read(Coverage.class, fhirId, getPracticeId());
            return fromFhirCoverage(updatedCoverage);
        } catch (Exception e) {
            log.warn("Could not reload updated coverage, returning patch DTO: {}", e.getMessage());
            patch.setFhirId(fhirId);
            patch.setExternalId(fhirId);
            
            try {
                patch.setId(Long.valueOf(fhirId));
            } catch (NumberFormatException ex) {
                patch.setId((long) fhirId.hashCode());
            }
            
            return patch;
        }
    }

    public void deleteItem(Long patientId, Long coverageId) {
        String fhirId = String.valueOf(coverageId);
        log.info("Deleting FHIR Coverage with ID: {}", fhirId);
        
        // Check if resource exists first
        try {
            fhirClientService.read(Coverage.class, fhirId, getPracticeId());
        } catch (ResourceNotFoundException e) {
            throw new RuntimeException("Coverage not found for patientId=" + patientId + " coverageId=" + coverageId);
        }
        
        fhirClientService.delete(Coverage.class, fhirId, getPracticeId());
    }

    public ApiResponse<List<CoverageDto>> searchAll() {
        log.debug("Searching all FHIR Coverages");

        Bundle bundle = fhirClientService.search(Coverage.class, getPracticeId());
        List<CoverageDto> coverages = extractCoverages(bundle);

        return ApiResponse.<List<CoverageDto>>builder()
                .success(true)
                .message("Coverages retrieved successfully")
                .data(coverages)
                .build();
    }

    private Coverage toFhirCoverage(CoverageDto dto) {
        Coverage coverage = new Coverage();
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);

        if (dto.getPatientId() != null) {
            coverage.setBeneficiary(new Reference("Patient/" + dto.getPatientId()));
        }

        if (dto.getPolicyNumber() != null) {
            coverage.addIdentifier()
                    .setSystem("http://ciyex.com/fhir/coverage-policy")
                    .setValue(dto.getPolicyNumber());
        }

        // Add all other fields as extensions
        addExtension(coverage, "planName", dto.getPlanName());
        addExtension(coverage, "coverageType", dto.getCoverageType());
        addExtension(coverage, "coverageStartDate", dto.getCoverageStartDate());
        addExtension(coverage, "coverageEndDate", dto.getCoverageEndDate());
        addExtension(coverage, "provider", dto.getProvider());
        addExtension(coverage, "effectiveDate", dto.getEffectiveDate());
        addExtension(coverage, "effectiveDateEnd", dto.getEffectiveDateEnd());
        addExtension(coverage, "groupNumber", dto.getGroupNumber());
        addExtension(coverage, "subscriberEmployer", dto.getSubscriberEmployer());
        addExtension(coverage, "subscriberAddressLine1", dto.getSubscriberAddressLine1());
        addExtension(coverage, "subscriberAddressLine2", dto.getSubscriberAddressLine2());
        addExtension(coverage, "subscriberCity", dto.getSubscriberCity());
        addExtension(coverage, "subscriberState", dto.getSubscriberState());
        addExtension(coverage, "subscriberZipCode", dto.getSubscriberZipCode());
        addExtension(coverage, "subscriberCountry", dto.getSubscriberCountry());
        addExtension(coverage, "subscriberPhone", dto.getSubscriberPhone());
        addExtension(coverage, "byholderName", dto.getByholderName());
        addExtension(coverage, "byholderRelation", dto.getByholderRelation());
        addExtension(coverage, "byholderAddressLine1", dto.getByholderAddressLine1());
        addExtension(coverage, "byholderAddressLine2", dto.getByholderAddressLine2());
        addExtension(coverage, "byholderCity", dto.getByholderCity());
        addExtension(coverage, "byholderState", dto.getByholderState());
        addExtension(coverage, "byholderZipCode", dto.getByholderZipCode());
        addExtension(coverage, "byholderCountry", dto.getByholderCountry());
        addExtension(coverage, "byholderPhone", dto.getByholderPhone());
        addExtension(coverage, "cardFrontUrl", dto.getCardFrontUrl());
        addExtension(coverage, "cardBackUrl", dto.getCardBackUrl());
        
        if (dto.getCopayAmount() != null) {
            coverage.addExtension("http://ciyex.com/fhir/coverage/copayAmount", new DecimalType(dto.getCopayAmount()));
        }

        // Link to insurance company if provided
        if (dto.getInsuranceCompany() != null && dto.getInsuranceCompany().getFhirId() != null) {
            coverage.addExtension("http://ciyex.com/fhir/coverage/insuranceCompanyId", new StringType(dto.getInsuranceCompany().getFhirId()));
        }

        return coverage;
    }

    private CoverageDto fromFhirCoverage(Coverage coverage) {
        CoverageDto dto = new CoverageDto();
        
        // FHIR ID
        if (coverage.hasId()) {
            String fhirId = coverage.getIdElement().getIdPart();
            dto.setFhirId(fhirId);
            dto.setExternalId(fhirId);
            
            // Set ID from FHIR ID
            try {
                dto.setId(Long.valueOf(fhirId));
            } catch (NumberFormatException e) {
                dto.setId((long) fhirId.hashCode());
            }
        }
        
        // Patient ID
        if (coverage.hasBeneficiary() && coverage.getBeneficiary().hasReference()) {
            String ref = coverage.getBeneficiary().getReference();
            if (ref.startsWith("Patient/")) {
                try {
                    dto.setPatientId(Long.parseLong(ref.substring(8)));
                } catch (NumberFormatException e) {
                    // Non-numeric FHIR ID
                }
            }
        }
        
        if (coverage.hasIdentifier()) {
            dto.setPolicyNumber(coverage.getIdentifierFirstRep().getValue());
        }
        
        // Extract all fields from extensions
        dto.setPlanName(getExtensionValue(coverage, "planName"));
        dto.setCoverageType(getExtensionValue(coverage, "coverageType"));
        dto.setCoverageStartDate(getExtensionValue(coverage, "coverageStartDate"));
        dto.setCoverageEndDate(getExtensionValue(coverage, "coverageEndDate"));
        dto.setProvider(getExtensionValue(coverage, "provider"));
        dto.setEffectiveDate(getExtensionValue(coverage, "effectiveDate"));
        dto.setEffectiveDateEnd(getExtensionValue(coverage, "effectiveDateEnd"));
        dto.setGroupNumber(getExtensionValue(coverage, "groupNumber"));
        dto.setSubscriberEmployer(getExtensionValue(coverage, "subscriberEmployer"));
        dto.setSubscriberAddressLine1(getExtensionValue(coverage, "subscriberAddressLine1"));
        dto.setSubscriberAddressLine2(getExtensionValue(coverage, "subscriberAddressLine2"));
        dto.setSubscriberCity(getExtensionValue(coverage, "subscriberCity"));
        dto.setSubscriberState(getExtensionValue(coverage, "subscriberState"));
        dto.setSubscriberZipCode(getExtensionValue(coverage, "subscriberZipCode"));
        dto.setSubscriberCountry(getExtensionValue(coverage, "subscriberCountry"));
        dto.setSubscriberPhone(getExtensionValue(coverage, "subscriberPhone"));
        dto.setByholderName(getExtensionValue(coverage, "byholderName"));
        dto.setByholderRelation(getExtensionValue(coverage, "byholderRelation"));
        dto.setByholderAddressLine1(getExtensionValue(coverage, "byholderAddressLine1"));
        dto.setByholderAddressLine2(getExtensionValue(coverage, "byholderAddressLine2"));
        dto.setByholderCity(getExtensionValue(coverage, "byholderCity"));
        dto.setByholderState(getExtensionValue(coverage, "byholderState"));
        dto.setByholderZipCode(getExtensionValue(coverage, "byholderZipCode"));
        dto.setByholderCountry(getExtensionValue(coverage, "byholderCountry"));
        dto.setByholderPhone(getExtensionValue(coverage, "byholderPhone"));
        dto.setCardFrontUrl(getExtensionValue(coverage, "cardFrontUrl"));
        dto.setCardBackUrl(getExtensionValue(coverage, "cardBackUrl"));
        
        Extension copayExt = coverage.getExtensionByUrl("http://ciyex.com/fhir/coverage/copayAmount");
        if (copayExt != null && copayExt.getValue() instanceof DecimalType) {
            dto.setCopayAmount(((DecimalType) copayExt.getValue()).getValueAsNumber().doubleValue());
        }
        
        // Load insurance company if referenced
        Extension insurerExt = coverage.getExtensionByUrl("http://ciyex.com/fhir/coverage/insuranceCompanyId");
        if (insurerExt != null && insurerExt.getValue() instanceof StringType) {
            String insurerId = ((StringType) insurerExt.getValue()).getValue();
            log.debug("Attempting to load insurance company with ID: {}", insurerId);
            try {
                InsuranceCompanyDto insuranceCompany = insuranceCompanyService.getById(insurerId);
                log.debug("Successfully loaded insurance company: {}", insuranceCompany.getName());
                dto.setInsuranceCompany(insuranceCompany);
            } catch (Exception e) {
                log.error("Failed to load insurance company {}: {}", insurerId, e.getMessage(), e);
                dto.setInsuranceCompany(null);
            }
        }
        
        // Add audit information
        CoverageDto.Audit audit = new CoverageDto.Audit();
        if (coverage.getMeta() != null && coverage.getMeta().hasLastUpdated()) {
            String timestamp = coverage.getMeta().getLastUpdated().toInstant().toString();
            audit.setLastModifiedDate(timestamp);
            audit.setCreatedDate(timestamp);
        } else {
            String currentTime = java.time.Instant.now().toString();
            audit.setCreatedDate(currentTime);
            audit.setLastModifiedDate(currentTime);
        }
        dto.setAudit(audit);
        
        return dto;
    }

    private List<CoverageDto> extractCoverages(Bundle bundle) {
        List<CoverageDto> items = new ArrayList<>();
        if (bundle.hasEntry()) {
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                if (entry.hasResource() && entry.getResource() instanceof Coverage) {
                    items.add(fromFhirCoverage((Coverage) entry.getResource()));
                }
            }
        }
        return items;
    }
    
    private void addExtension(Coverage coverage, String field, String value) {
        if (value != null) {
            coverage.addExtension("http://ciyex.com/fhir/coverage/" + field, new StringType(value));
        }
    }
    
    private String getExtensionValue(Coverage coverage, String field) {
        Extension ext = coverage.getExtensionByUrl("http://ciyex.com/fhir/coverage/" + field);
        if (ext != null && ext.getValue() instanceof StringType) {
            return ((StringType) ext.getValue()).getValue();
        }
        return null;
    }
}