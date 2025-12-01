package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Entity
@Table(name = "credit_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class CreditCard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "fhir_id")
    private String fhirId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "card_holder_name", nullable = false, length = 100)
    private String cardHolderName;

    @Column(name = "card_number", nullable = false, length = 16)
    private String cardNumber; // Should be encrypted in production

    @Column(name = "card_type", length = 20)
    private String cardType; // VISA, MASTERCARD, AMEX, DISCOVER

    @Column(name = "expiry_month", nullable = false)
    private Integer expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private Integer expiryYear;

    @Column(name = "cvv", length = 4)
    private String cvv; // Should be encrypted in production

    @Column(name = "billing_address")
    private String billingAddress;

    @Column(name = "billing_city", length = 50)
    private String billingCity;

    @Column(name = "billing_state", length = 50)
    private String billingState;

    @Column(name = "billing_zip", length = 10)
    private String billingZip;

    @Column(name = "billing_country", length = 50)
    private String billingCountry;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "token")
    private String token; // Payment gateway token if tokenized

    // Relationship with Patient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", insertable = false, updatable = false)
    private Patient patient;

    // Helper method to get masked card number
    @Transient
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 4) {
            return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
        }
        return "****";
    }

    // Helper method to check if card is expired
    @Transient
    public boolean isExpired() {
        LocalDate now = LocalDate.now();
        LocalDate expiry = LocalDate.of(expiryYear, expiryMonth, 1).plusMonths(1).minusDays(1);
        return now.isAfter(expiry);
    }
}
