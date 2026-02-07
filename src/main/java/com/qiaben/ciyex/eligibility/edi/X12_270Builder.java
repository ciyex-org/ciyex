package com.qiaben.ciyex.eligibility.edi;

import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class X12_270Builder {
    
    public String build(String firstName, String lastName, String dob, String memberId, 
                       String payerId, String payerName, String serviceTypeCode,
                       String senderId, String receiverId) {
        String controlNumber = String.format("%09d", System.currentTimeMillis() % 1000000000);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmm"));
        
        return String.format(
            "ISA*00*          *00*          *ZZ*%-15s*ZZ*%-15s*%s*%s*U*00401*%s*0*P*:~" +
            "GS*HS*%s*%s*%s*%s*%s*X*004010X092~" +
            "ST*270*%s~" +
            "BHT*0022*13*%s*%s*%s~" +
            "HL*1**20*1~" +
            "NM1*PR*2*%s*****PI*%s~" +
            "HL*2*1*21*1~" +
            "HL*3*2*22*0~" +
            "TRN*1*%s*%s~" +
            "NM1*IL*1*%s*%s*****MI*%s~" +
            "DMG*D8*%s~" +
            "DTP*291*D8*%s~" +
            "EQ*%s~" +
            "SE*13*%s~" +
            "GE*1*%s~" +
            "IEA*1*%s~",
            senderId, receiverId, date, time, controlNumber,
            senderId, receiverId, date, time, controlNumber,
            controlNumber,
            controlNumber, date, time,
            payerName != null ? payerName : "PAYER", payerId,
            controlNumber, senderId,
            lastName, firstName, memberId,
            dob,
            date,
            serviceTypeCode != null ? serviceTypeCode : "30",
            controlNumber,
            controlNumber,
            controlNumber
        );
    }
}
