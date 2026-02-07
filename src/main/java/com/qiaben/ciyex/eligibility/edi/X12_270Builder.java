package com.qiaben.ciyex.eligibility.edi;

import com.qiaben.ciyex.eligibility.dto.EligibilityRequestDto;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class X12_270Builder {
    
    private static final String SEGMENT_DELIMITER = "~";
    private static final String ELEMENT_DELIMITER = "*";
    
    public String build(EligibilityRequestDto request, String senderId, String receiverId) {
        StringBuilder x12 = new StringBuilder();
        String controlNumber = generateControlNumber();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"));
        
        // ISA - Interchange Control Header
        x12.append("ISA*00*          *00*          *ZZ*")
           .append(padRight(senderId, 15))
           .append("*ZZ*")
           .append(padRight(receiverId, 15))
           .append("*").append(timestamp)
           .append("*").append(time)
           .append("*U*00401*").append(controlNumber)
           .append("*0*P*:").append(SEGMENT_DELIMITER);
        
        // GS - Functional Group Header
        x12.append("GS*HS*").append(senderId)
           .append("*").append(receiverId)
           .append("*").append(timestamp)
           .append("*").append(time)
           .append("*").append(controlNumber)
           .append("*X*004010X092").append(SEGMENT_DELIMITER);
        
        // ST - Transaction Set Header
        x12.append("ST*270*").append(controlNumber).append(SEGMENT_DELIMITER);
        
        // BHT - Beginning of Hierarchical Transaction
        x12.append("BHT*0022*13*").append(controlNumber)
           .append("*").append(timestamp)
           .append("*").append(time).append(SEGMENT_DELIMITER);
        
        // HL - Information Source Level
        x12.append("HL*1**20*1").append(SEGMENT_DELIMITER);
        
        // NM1 - Information Source Name (Payer)
        x12.append("NM1*PR*2*").append(request.getPayerName() != null ? request.getPayerName() : "PAYER")
           .append("*****PI*").append(request.getPayerId()).append(SEGMENT_DELIMITER);
        
        // HL - Information Receiver Level
        x12.append("HL*2*1*21*1").append(SEGMENT_DELIMITER);
        
        // NM1 - Information Receiver Name (Provider)
        if (request.getProviderNpi() != null) {
            x12.append("NM1*1P*2*PROVIDER*****XX*").append(request.getProviderNpi()).append(SEGMENT_DELIMITER);
        }
        
        // HL - Subscriber Level
        x12.append("HL*3*2*22*0").append(SEGMENT_DELIMITER);
        
        // TRN - Trace Number
        x12.append("TRN*1*").append(controlNumber).append("*").append(senderId).append(SEGMENT_DELIMITER);
        
        // NM1 - Subscriber Name
        x12.append("NM1*IL*1*").append(request.getPatientLastName())
           .append("*").append(request.getPatientFirstName())
           .append("*****MI*").append(request.getMemberId()).append(SEGMENT_DELIMITER);
        
        // DMG - Subscriber Demographics
        x12.append("DMG*D8*").append(request.getPatientDob()).append(SEGMENT_DELIMITER);
        
        // DTP - Date Time Period
        x12.append("DTP*291*D8*").append(timestamp).append(SEGMENT_DELIMITER);
        
        // EQ - Eligibility or Benefit Inquiry
        String serviceType = request.getServiceTypeCode() != null ? request.getServiceTypeCode() : "30";
        x12.append("EQ*").append(serviceType).append(SEGMENT_DELIMITER);
        
        // SE - Transaction Set Trailer
        int segmentCount = countSegments(x12.toString()) + 1;
        x12.append("SE*").append(segmentCount).append("*").append(controlNumber).append(SEGMENT_DELIMITER);
        
        // GE - Functional Group Trailer
        x12.append("GE*1*").append(controlNumber).append(SEGMENT_DELIMITER);
        
        // IEA - Interchange Control Trailer
        x12.append("IEA*1*").append(controlNumber).append(SEGMENT_DELIMITER);
        
        return x12.toString();
    }
    
    private String generateControlNumber() {
        return String.format("%09d", System.currentTimeMillis() % 1000000000);
    }
    
    private String padRight(String str, int length) {
        return String.format("%-" + length + "s", str).substring(0, length);
    }
    
    private int countSegments(String x12) {
        return x12.split("~").length;
    }
}
