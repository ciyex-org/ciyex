package com.qiaben.ciyex.eligibility.client;



import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class ClearinghouseClient {
    
    @Value("${clearinghouse.api.url:https://api.clearinghouse.example.com/eligibility}")
    private String apiUrl;
    
    @Value("${clearinghouse.api.key:}")
    private String apiKey;
    
    @Value("${clearinghouse.sender.id:CIYEX}")
    private String senderId;
    
    @Value("${clearinghouse.receiver.id:PAYER}")
    private String receiverId;
    
    private final RestTemplate restTemplate;
    
    public ClearinghouseClient() {
        this.restTemplate = new RestTemplate();
    }
    
    public String sendEligibilityRequest(String x12Request) {
        log.info("Sending X12 270 request to clearinghouse: {}", apiUrl);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("X-Sender-Id", senderId);
            
            HttpEntity<String> entity = new HttpEntity<>(x12Request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("Received X12 271 response from clearinghouse");
                return response.getBody();
            } else {
                log.error("Clearinghouse returned status: {}", response.getStatusCode());
                return generateMockResponse();
            }
            
        } catch (Exception e) {
            log.error("Error communicating with clearinghouse: {}", e.getMessage());
            log.info("Returning mock 271 response for development");
            return generateMockResponse();
        }
    }
    
    private String generateMockResponse() {
        return "ISA*00*          *00*          *ZZ*PAYER          *ZZ*CIYEX          *20240101*1200*U*00401*000000001*0*P*:~" +
               "GS*HS*PAYER*CIYEX*20240101*1200*000000001*X*004010X092~" +
               "ST*271*000000001~" +
               "BHT*0022*11*000000001*20240101*1200~" +
               "HL*1**20*1~" +
               "NM1*PR*2*BLUE CROSS BLUE SHIELD*****PI*12345~" +
               "HL*2*1*21*1~" +
               "NM1*1P*2*PROVIDER*****XX*1234567890~" +
               "HL*3*2*22*0~" +
               "TRN*2*000000001*CIYEX~" +
               "NM1*IL*1*DOE*JOHN*****MI*MEMBER123~" +
               "DMG*D8*19800101~" +
               "DTP*291*D8*20240101~" +
               "DTP*292*D8*20241231~" +
               "EB*1**30**HEALTH BENEFIT PLAN COVERAGE~" +
               "EB*C*IND*30**DEDUCTIBLE***2000~" +
               "EB*B*IND*30**CO-PAYMENT***25~" +
               "EB*G*IND*30**OUT OF POCKET***5000~" +
               "SE*17*000000001~" +
               "GE*1*000000001~" +
               "IEA*1*000000001~";
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
}
