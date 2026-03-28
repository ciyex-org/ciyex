package org.ciyex.ehr.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "payment_config")
@Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String processor;           // stripe, square, authorize_net
    private Boolean enabled;
    @Column(columnDefinition = "JSONB")
    private String config;              // {stripe_publishable_key, stripe_secret_key, webhook_secret}
    @Column(columnDefinition = "JSONB")
    private String acceptedMethods;     // ["credit_card","debit_card","bank_account","fsa","hsa"]
    private Boolean convenienceFeeEnabled;
    private BigDecimal convenienceFeePercent;
    private BigDecimal convenienceFeeFlat;
    private Boolean autoReceipt;
    private Long receiptEmailTemplateId;
    private String orgAlias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (processor == null) processor = "stripe";
        if (enabled == null) enabled = false;
        if (config == null) config = "{}";
        if (convenienceFeeEnabled == null) convenienceFeeEnabled = false;
        if (autoReceipt == null) autoReceipt = true;
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
