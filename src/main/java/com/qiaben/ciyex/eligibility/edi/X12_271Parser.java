package com.qiaben.ciyex.eligibility.edi;



import com.qiaben.ciyex.eligibility.dto.EligibilityResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class X12_271Parser {
    
    private static final Map<String, String> SERVICE_TYPES = Map.of(
        "1", "Medical Care",
        "30", "Health Benefit Plan Coverage",
        "33", "Chiropractic",
        "35", "Dental Care",
        "47", "Hospital - Inpatient",
        "48", "Hospital - Outpatient",
        "50", "Hospital - Emergency",
        "86", "Emergency Services",
        "88", "Pharmacy",
        "98", "Professional (Physician Visit)"
    );
    
    public EligibilityResponseDto parse(String x12Response) {
        EligibilityResponseDto response = new EligibilityResponseDto();
        response.setRawX12Response(x12Response);
        response.setServiceCoverages(new ArrayList<>());
        response.setStatus("Active");
        
        String[] segments = x12Response.split("~");
        
        for (String segment : segments) {
            String[] elements = segment.trim().split("\\*");
            if (elements.length == 0) continue;
            
            switch (elements[0]) {
                case "TRN" -> { if (elements.length > 2) response.setTransactionId(elements[2]); }
                case "NM1" -> parseNM1(elements, response);
                case "EB" -> parseEB(elements, response);
                case "DTP" -> parseDTP(elements, response);
                case "AAA" -> { if (elements.length > 1 && "N".equals(elements[1])) response.setStatus("Inactive"); }
            }
        }
        
        return response;
    }
    
    private void parseNM1(String[] e, EligibilityResponseDto r) {
        if (e.length < 4) return;
        if ("PR".equals(e[1])) r.setPayerName(e[3]);
        if ("IL".equals(e[1]) && e.length > 9) r.setMemberId(e[9]);
    }
    
    private void parseEB(String[] e, EligibilityResponseDto r) {
        if (e.length < 2) return;
        
        if ("1".equals(e[1])) r.setStatus("Active");
        else if ("6".equals(e[1])) r.setStatus("Inactive");
        
        String coverage = e.length > 4 ? e[4] : "";
        String amount = e.length > 7 ? e[7] : "";
        
        if (!amount.isEmpty()) {
            try {
                Double value = Double.parseDouble(amount);
                switch (coverage) {
                    case "C" -> r.setDeductibleAmount(value);
                    case "G" -> r.setOutOfPocketMax(value);
                    case "B" -> r.setCopayAmount(value);
                }
            } catch (NumberFormatException ignored) {}
        }
        
        var sc = new EligibilityResponseDto.ServiceCoverage();
        sc.setServiceTypeCode(e.length > 3 ? e[3] : "");
        sc.setServiceType(SERVICE_TYPES.getOrDefault(sc.getServiceTypeCode(), "Unknown Service"));
        sc.setCoverageLevel(coverage);
        if (!amount.isEmpty()) try { sc.setCopay(Double.parseDouble(amount)); } catch (NumberFormatException ignored) {}
        r.getServiceCoverages().add(sc);
    }
    
    private void parseDTP(String[] e, EligibilityResponseDto r) {
        if (e.length < 4) return;
        String date = e[3].length() == 8 ? e[3].substring(0, 4) + "-" + e[3].substring(4, 6) + "-" + e[3].substring(6, 8) : e[3];
        if ("291".equals(e[1])) r.setCoverageStartDate(date);
        else if ("292".equals(e[1])) r.setCoverageEndDate(date);
    }
}
