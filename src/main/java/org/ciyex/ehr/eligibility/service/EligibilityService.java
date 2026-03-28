package org.ciyex.ehr.eligibility.service;

import org.ciyex.ehr.eligibility.client.ClearinghouseClient;
import org.ciyex.ehr.eligibility.dto.EligibilityResponseDto;
import org.ciyex.ehr.eligibility.edi.X12_270Builder;
import org.ciyex.ehr.eligibility.edi.X12_271Parser;
import org.ciyex.ehr.eligibility.repository.EligibilityRepository;
import org.ciyex.ehr.fhir.GenericFhirResourceService;
import org.ciyex.ehr.service.PracticeContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EligibilityService {

    private final X12_270Builder x12Builder;
    private final X12_271Parser x12Parser;
    private final ClearinghouseClient clearinghouseClient;
    private final EligibilityRepository repository;
    private final PracticeContextService practiceContextService;
    private final GenericFhirResourceService fhirService;

    public EligibilityService(
            X12_270Builder x12Builder,
            X12_271Parser x12Parser,
            ClearinghouseClient clearinghouseClient,
            EligibilityRepository repository,
            PracticeContextService practiceContextService,
            GenericFhirResourceService fhirService) {
        this.x12Builder = x12Builder;
        this.x12Parser = x12Parser;
        this.clearinghouseClient = clearinghouseClient;
        this.repository = repository;
        this.practiceContextService = practiceContextService;
        this.fhirService = fhirService;
    }

    @SuppressWarnings("unchecked")
    public EligibilityResponseDto checkEligibility(Long patientId, String serviceTypeCode) {
        log.info("Starting eligibility check for patient: {}", patientId);

        // Get patient data via generic FHIR service (demographics tab)
        Map<String, Object> patient = fhirService.get("demographics", null, String.valueOf(patientId));
        if (patient == null) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        // Get coverage data via generic FHIR service (insurance tab)
        Map<String, Object> coverageResult = fhirService.list("insurance", patientId, 0, 1);
        List<Map<String, Object>> coverages = (List<Map<String, Object>>) coverageResult.get("content");
        if (coverages == null || coverages.isEmpty()) {
            throw new RuntimeException("No coverage found for patient: " + patientId);
        }
        Map<String, Object> coverage = coverages.get(0);

        String policyNumber = str(coverage, "policyNumber", "subscriberId");
        if (policyNumber == null) {
            throw new RuntimeException("No policy number found for patient: " + patientId);
        }

        String firstName = str(patient, "identification.firstName", "firstName");
        String lastName = str(patient, "identification.lastName", "lastName");
        String dob = str(patient, "demographics.dateOfBirth", "dateOfBirth");
        String payerId = str(coverage, "insuranceCompany.payerId", "payerId");
        String provider = str(coverage, "provider");

        // Build X12 270 request
        String x12Request = x12Builder.build(
                firstName,
                lastName,
                dob != null ? dob.replace("-", "") : "",
                policyNumber,
                payerId != null ? payerId : "UNKNOWN",
                provider,
                serviceTypeCode != null ? serviceTypeCode : "30",
                clearinghouseClient.getSenderId(),
                clearinghouseClient.getReceiverId()
        );

        log.debug("Generated X12 270 request");

        // Send to clearinghouse
        String x12Response = clearinghouseClient.sendEligibilityRequest(x12Request);
        log.debug("Received X12 271 response");

        // Parse X12 271 response
        EligibilityResponseDto response = x12Parser.parse(x12Response);

        // Store transaction in FHIR
        try {
            repository.save(
                    response.getTransactionId(),
                    patientId,
                    policyNumber,
                    payerId,
                    response,
                    x12Request,
                    x12Response,
                    practiceContextService.getPracticeId()
            );
        } catch (Exception e) {
            log.error("Failed to save eligibility transaction: {}", e.getMessage());
        }

        log.info("Eligibility check completed. Status: {}", response.getStatus());
        return response;
    }

    private String str(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof String s && !s.isBlank()) return s;
            if (val != null) return val.toString();
        }
        return null;
    }
}
