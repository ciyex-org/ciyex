package com.qiaben.ciyex.eligibility.edi;

import com.qiaben.ciyex.eligibility.dto.EligibilityResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
@Slf4j
public class X12_271Parser {
    
    public EligibilityResponseDto parse(String x12Response) {
        EligibilityResponseDto response = new EligibilityResponseDto();
        response.setRawX12Response(x12Response);
        response.setServiceCoverages(new ArrayList<>());
        
        String[] segments = x12Response.split("~");
        
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            String[] elements = segment.split("\\*");
            
            if (elements.length == 0) continue;
            
            String segmentId = elements[0];
            
            switch (segmentId) {
                case "TRN":
                    if (elements.length > 2) response.setTransactionId(elements[2]);
                    break;
                    
                case "NM1":
                    parseNM1(elements, response);
                    break;
                    
                case "EB":
                    parseEB(elements, response);
                    break;
                    
                case "DTP":
                    parseDTP(elements, response, i > 0 ? segments[i-1] : "");
                    break;
                    
                case "AAA":
                    if (elements.length > 3 && "N".equals(elements[1])) {
                        response.setStatus("Inactive");
                    }
                    break;
            }
        }
        
        if (response.getStatus() == null) {
            response.setStatus("Active");
        }
        
        return response;
    }
    
    private void parseNM1(String[] elements, EligibilityResponseDto response) {
        if (elements.length < 4) return;
        
        String entityCode = elements[1];
        
        // PR = Payer
        if ("PR".equals(entityCode) && elements.length > 3) {
            response.setPayerName(elements[3]);
        }
        
        // IL = Insured/Subscriber
        if ("IL".equals(entityCode) && elements.length > 9) {
            response.setMemberId(elements[9]);
        }
    }
    
    private void parseEB(String[] elements, EligibilityResponseDto response) {
        if (elements.length < 2) return;
        
        String eligibilityCode = elements[1]; // 1=Active, 6=Inactive
        String serviceTypeCode = elements.length > 3 ? elements[3] : "";
        String planCoverage = elements.length > 4 ? elements[4] : "";
        String timePeriod = elements.length > 6 ? elements[6] : "";
        String monetaryAmount = elements.length > 7 ? elements[7] : "";
        
        // Set overall status
        if ("1".equals(eligibilityCode)) {
            response.setStatus("Active");
        } else if ("6".equals(eligibilityCode)) {
            response.setStatus("Inactive");
        }
        
        // Parse monetary amounts
        if (!monetaryAmount.isEmpty()) {
            try {
                Double amount = Double.parseDouble(monetaryAmount);
                
                // Map service type codes to amounts
                if ("C".equals(planCoverage)) { // Deductible
                    response.setDeductibleAmount(amount);
                } else if ("G".equals(planCoverage)) { // Out of Pocket
                    response.setOutOfPocketMax(amount);
                } else if ("B".equals(planCoverage)) { // Co-Payment
                    response.setCopayAmount(amount);
                }
            } catch (NumberFormatException e) {
                log.warn("Invalid monetary amount: {}", monetaryAmount);
            }
        }
        
        // Create service coverage entry
        EligibilityResponseDto.ServiceCoverage serviceCoverage = new EligibilityResponseDto.ServiceCoverage();
        serviceCoverage.setServiceTypeCode(serviceTypeCode);
        serviceCoverage.setServiceType(mapServiceType(serviceTypeCode));
        serviceCoverage.setCoverageLevel(planCoverage);
        serviceCoverage.setTimePeriod(timePeriod);
        
        if (!monetaryAmount.isEmpty()) {
            try {
                serviceCoverage.setCopay(Double.parseDouble(monetaryAmount));
            } catch (NumberFormatException ignored) {}
        }
        
        response.getServiceCoverages().add(serviceCoverage);
    }
    
    private void parseDTP(String[] elements, EligibilityResponseDto response, String previousSegment) {
        if (elements.length < 4) return;
        
        String dateQualifier = elements[1];
        String date = elements[3];
        
        // 291 = Plan Begin, 292 = Plan End
        if ("291".equals(dateQualifier)) {
            response.setCoverageStartDate(formatDate(date));
        } else if ("292".equals(dateQualifier)) {
            response.setCoverageEndDate(formatDate(date));
        }
    }
    
    private String formatDate(String date) {
        if (date == null || date.length() != 8) return date;
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }
    
    private String mapServiceType(String code) {
        Map<String, String> serviceTypes = new HashMap<>();
        serviceTypes.put("1", "Medical Care");
        serviceTypes.put("30", "Health Benefit Plan Coverage");
        serviceTypes.put("33", "Chiropractic");
        serviceTypes.put("35", "Dental Care");
        serviceTypes.put("47", "Hospital - Inpatient");
        serviceTypes.put("48", "Hospital - Outpatient");
        serviceTypes.put("50", "Hospital - Emergency");
        serviceTypes.put("86", "Emergency Services");
        serviceTypes.put("88", "Pharmacy");
        serviceTypes.put("98", "Professional (Physician Visit)");
        
        return serviceTypes.getOrDefault(code, "Unknown Service");
    }
}
