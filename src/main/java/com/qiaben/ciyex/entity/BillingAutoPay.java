package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_autopay")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAutoPay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private Long userId;

    private Boolean enabled;

    private LocalDate startDate;

    private String frequency; // Monthly / Quarterly / Yearly

    private Double maxAmount;

    private Long cardId; // FK to BillingCard

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
