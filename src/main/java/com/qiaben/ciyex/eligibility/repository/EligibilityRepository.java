package com.qiaben.ciyex.eligibility.repository;



import com.qiaben.ciyex.eligibility.dto.EligibilityResponseDto;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Repository
@Slf4j
public class EligibilityRepository {
    
    private final FhirClientService fhirClientService;
    
    public EligibilityRepository(FhirClientService fhirClientService) {
        this.fhirClientService = fhirClientService;
    }
    
    public String save(String transactionId, Long patientId, String memberId, String payerId, 
                       EligibilityResponseDto response, String x12Request, String x12Response, 
                       String practiceId) {
        Basic resource = toFhirBasic(transactionId, patientId, memberId, payerId, response, x12Request, x12Response);
        
        try {
            var outcome = fhirClientService.create(resource, practiceId);
            String fhirId = outcome.getId().getIdPart();
            log.info("Saved eligibility transaction: {}", fhirId);
            return fhirId;
        } catch (Exception e) {
            log.error("Failed to save eligibility transaction: {}", e.getMessage());
            return null;
        }
    }
    
    public Optional<Map<String, Object>> findByTransactionId(String transactionId, String practiceId) {
        try {
            Bundle bundle = fhirClientService.getClient(practiceId).search()
                .forResource(Basic.class)
                .where(Basic.IDENTIFIER.exactly().code(transactionId))
                .returnBundle(Bundle.class)
                .execute();
            
            if (bundle.hasEntry()) {
                Basic resource = (Basic) bundle.getEntryFirstRep().getResource();
                return Optional.of(fromFhirBasic(resource));
            }
        } catch (Exception e) {
            log.error("Error finding transaction: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    private Basic toFhirBasic(String transactionId, Long patientId, String memberId, String payerId,
                              EligibilityResponseDto response, String x12Request, String x12Response) {
        Basic basic = new Basic();
        basic.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/basic-type", "eligibility-transaction", "Eligibility Transaction")
        ));
        
        if (transactionId != null) {
            basic.addIdentifier().setValue(transactionId);
        }
        
        addExtension(basic, "patientId", patientId != null ? patientId.toString() : null);
        addExtension(basic, "memberId", memberId);
        addExtension(basic, "payerId", payerId);
        addExtension(basic, "payerName", response.getPayerName());
        addExtension(basic, "status", response.getStatus());
        addExtension(basic, "planName", response.getPlanName());
        addExtension(basic, "coverageStartDate", response.getCoverageStartDate());
        addExtension(basic, "coverageEndDate", response.getCoverageEndDate());
        addExtension(basic, "x12Request", x12Request);
        addExtension(basic, "x12Response", x12Response);
        
        if (response.getCopayAmount() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/copayAmount", new DecimalType(response.getCopayAmount()));
        }
        if (response.getDeductibleAmount() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/deductibleAmount", new DecimalType(response.getDeductibleAmount()));
        }
        if (response.getOutOfPocketMax() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/outOfPocketMax", new DecimalType(response.getOutOfPocketMax()));
        }
        
        return basic;
    }
    
    private Map<String, Object> fromFhirBasic(Basic basic) {
        Map<String, Object> data = new HashMap<>();
        
        if (basic.hasId()) {
            data.put("id", basic.getIdElement().getIdPart());
        }
        if (basic.hasIdentifier()) {
            data.put("transactionId", basic.getIdentifierFirstRep().getValue());
        }
        
        data.put("patientId", getLongExtension(basic, "patientId"));
        data.put("memberId", getExtension(basic, "memberId"));
        data.put("payerId", getExtension(basic, "payerId"));
        data.put("payerName", getExtension(basic, "payerName"));
        data.put("status", getExtension(basic, "status"));
        data.put("planName", getExtension(basic, "planName"));
        data.put("coverageStartDate", getExtension(basic, "coverageStartDate"));
        data.put("coverageEndDate", getExtension(basic, "coverageEndDate"));
        data.put("x12Request", getExtension(basic, "x12Request"));
        data.put("x12Response", getExtension(basic, "x12Response"));
        
        data.put("copayAmount", getDecimalExtension(basic, "copayAmount"));
        data.put("deductibleAmount", getDecimalExtension(basic, "deductibleAmount"));
        data.put("outOfPocketMax", getDecimalExtension(basic, "outOfPocketMax"));
        
        if (basic.getMeta() != null && basic.getMeta().hasLastUpdated()) {
            LocalDateTime timestamp = basic.getMeta().getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            data.put("responseTimestamp", timestamp);
        }
        
        return data;
    }
    
    private void addExtension(Basic basic, String field, String value) {
        if (value != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/" + field, new StringType(value));
        }
    }
    
    private String getExtension(Basic basic, String field) {
        Extension ext = basic.getExtensionByUrl("http://ciyex.com/fhir/eligibility/" + field);
        return ext != null && ext.getValue() instanceof StringType ? ((StringType) ext.getValue()).getValue() : null;
    }
    
    private Long getLongExtension(Basic basic, String field) {
        String value = getExtension(basic, field);
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double getDecimalExtension(Basic basic, String field) {
        Extension ext = basic.getExtensionByUrl("http://ciyex.com/fhir/eligibility/" + field);
        return ext != null && ext.getValue() instanceof DecimalType ? ((DecimalType) ext.getValue()).getValueAsNumber().doubleValue() : null;
    }
}
