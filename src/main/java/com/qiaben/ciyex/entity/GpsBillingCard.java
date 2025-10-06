package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gps_billing_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GpsBillingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "gps_customer_vault_id", length = 64)
    private String gpsCustomerVaultId;

    @Column(length = 20)
    private String brand;

    @Column(length = 4)
    private String last4;

    @Column(name = "exp_month")
    private Integer expMonth;

    @Column(name = "exp_year")
    private Integer expYear;

    @Column(name = "is_default")
    private boolean isDefault;

    // Cardholder info
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    // Billing address
    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 50)
    private String state;

    @Column(name = "zip", length = 20)
    private String zip;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
