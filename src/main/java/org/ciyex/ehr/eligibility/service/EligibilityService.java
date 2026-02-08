package org.ciyex.ehr.eligibility.service;



import org.ciyex.ehr.dto.CoverageDto;
import org.ciyex.ehr.dto.PatientDto;
import org.ciyex.ehr.eligibility.client.ClearinghouseClient;
import org.ciyex.ehr.eligibility.dto.EligibilityResponseDto;
import org.ciyex.ehr.eligibility.edi.X12_270Builder;
import org.ciyex.ehr.eligibility.edi.X12_271Parser;
import org.ciyex.ehr.eligibility.repository.EligibilityRepository;
import org.ciyex.ehr.service.CoverageService;
import org.ciyex.ehr.service.PatientService;
import org.ciyex.ehr.service.PracticeContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EligibilityService {
    
    private final X12_270Builder x12Builder;
    private final X12_271Parser x12Parser;
    private final ClearinghouseClient clearinghouseClient;
    private final EligibilityRepository repository;
    private final PracticeContextService practiceContextService;
    private final PatientService patientService;
    private final CoverageService coverageService;
    
    public EligibilityService(
            X12_270Builder x12Builder,
            X12_271Parser x12Parser,
            ClearinghouseClient clearinghouseClient,
            EligibilityRepository repository,
            PracticeContextService practiceContextService,
            PatientService patientService,
            CoverageService coverageService) {
        this.x12Builder = x12Builder;
        this.x12Parser = x12Parser;
        this.clearinghouseClient = clearinghouseClient;
        this.repository = repository;
        this.practiceContextService = practiceContextService;
        this.patientService = patientService;
        this.coverageService = coverageService;
    }
    
    public EligibilityResponseDto checkEligibility(Long patientId, String serviceTypeCode) {
        log.info("Starting eligibility check for patient: {}", patientId);
        
        // Get patient data
        PatientDto patient = patientService.getById(patientId);
        if (patient == null) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        
        // Get coverage data
        CoverageDto coverage = coverageService.getByPatientId(patientId);
        if (coverage == null || coverage.getPolicyNumber() == null) {
            throw new RuntimeException("No coverage found for patient: " + patientId);
        }
        
        // Build X12 270 request
        String x12Request = x12Builder.build(
            patient.getFirstName(),
            patient.getLastName(),
            patient.getDateOfBirth().replace("-", ""),
            coverage.getPolicyNumber(),
            coverage.getInsuranceCompany() != null ? coverage.getInsuranceCompany().getPayerId() : "UNKNOWN",
            coverage.getProvider(),
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
                coverage.getPolicyNumber(),
                coverage.getInsuranceCompany() != null ? coverage.getInsuranceCompany().getPayerId() : null,
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
}
