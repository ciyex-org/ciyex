package org.ciyex.ehr.payment.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentConfigDto {
    private Long id;
    private String processor;           // stripe, square, authorize_net
    private Boolean enabled;
    private String config;              // JSONB as String
    private String acceptedMethods;     // JSONB as String
    private Boolean convenienceFeeEnabled;
    private BigDecimal convenienceFeePercent;
    private BigDecimal convenienceFeeFlat;
    private Boolean autoReceipt;
    private Long receiptEmailTemplateId;
    private String createdAt;
    private String updatedAt;
}
