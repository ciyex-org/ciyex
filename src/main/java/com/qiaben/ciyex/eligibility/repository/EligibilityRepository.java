package com.qiaben.ciyex.eligibility.repository;

import com.qiaben.ciyex.eligibility.entity.EligibilityTransaction;
import com.qiaben.ciyex.fhir.FhirClientService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Repository;
import java.time.ZoneId;
import java.util.*;

@Repository
@Slf4j
public class EligibilityRepository {
    
    private final FhirClientService fhirClientService;
    
    public EligibilityRepository(FhirClientService fhirClientService) {
        this.fhirClientService = fhirClientService;
    }
    
    public EligibilityTransaction save(EligibilityTransaction transaction, String practiceId) {
        Basic resource = toFhirBasic(transaction);
        
        try {
            var outcome = fhirClientService.create(resource, practiceId);
            transaction.setId(outcome.getId().getIdPart());
            log.info("Saved eligibility transaction: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Failed to save eligibility transaction: {}", e.getMessage());
        }
        
        return transaction;
    }
    
    public Optional<EligibilityTransaction> findByTransactionId(String transactionId, String practiceId) {
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
    
    private Basic toFhirBasic(EligibilityTransaction tx) {
        Basic basic = new Basic();
        basic.setCode(new CodeableConcept().addCoding(
            new Coding("http://ciyex.com/fhir/basic-type", "eligibility-transaction", "Eligibility Transaction")
        ));
        
        if (tx.getTransactionId() != null) {
            basic.addIdentifier().setValue(tx.getTransactionId());
        }
        
        addExtension(basic, "patientId", tx.getPatientId() != null ? tx.getPatientId().toString() : null);
        addExtension(basic, "memberId", tx.getMemberId());
        addExtension(basic, "payerId", tx.getPayerId());
        addExtension(basic, "payerName", tx.getPayerName());
        addExtension(basic, "status", tx.getStatus());
        addExtension(basic, "planName", tx.getPlanName());
        addExtension(basic, "coverageStartDate", tx.getCoverageStartDate());
        addExtension(basic, "coverageEndDate", tx.getCoverageEndDate());
        addExtension(basic, "x12Request", tx.getX12Request());
        addExtension(basic, "x12Response", tx.getX12Response());
        
        if (tx.getCopayAmount() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/copayAmount", new DecimalType(tx.getCopayAmount()));
        }
        if (tx.getDeductibleAmount() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/deductibleAmount", new DecimalType(tx.getDeductibleAmount()));
        }
        if (tx.getOutOfPocketMax() != null) {
            basic.addExtension("http://ciyex.com/fhir/eligibility/outOfPocketMax", new DecimalType(tx.getOutOfPocketMax()));
        }
        
        return basic;
    }
    
    private EligibilityTransaction fromFhirBasic(Basic basic) {
        EligibilityTransaction tx = new EligibilityTransaction();
        
        if (basic.hasId()) {
            tx.setId(basic.getIdElement().getIdPart());
        }
        if (basic.hasIdentifier()) {
            tx.setTransactionId(basic.getIdentifierFirstRep().getValue());
        }
        
        tx.setPatientId(getLongExtension(basic, "patientId"));
        tx.setMemberId(getExtension(basic, "memberId"));
        tx.setPayerId(getExtension(basic, "payerId"));
        tx.setPayerName(getExtension(basic, "payerName"));
        tx.setStatus(getExtension(basic, "status"));
        tx.setPlanName(getExtension(basic, "planName"));
        tx.setCoverageStartDate(getExtension(basic, "coverageStartDate"));
        tx.setCoverageEndDate(getExtension(basic, "coverageEndDate"));
        tx.setX12Request(getExtension(basic, "x12Request"));
        tx.setX12Response(getExtension(basic, "x12Response"));
        
        tx.setCopayAmount(getDecimalExtension(basic, "copayAmount"));
        tx.setDeductibleAmount(getDecimalExtension(basic, "deductibleAmount"));
        tx.setOutOfPocketMax(getDecimalExtension(basic, "outOfPocketMax"));
        
        if (basic.getMeta() != null && basic.getMeta().hasLastUpdated()) {
            tx.setResponseTimestamp(basic.getMeta().getLastUpdated().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        return tx;
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
