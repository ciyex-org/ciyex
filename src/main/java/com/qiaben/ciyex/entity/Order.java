package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;

    private String orderNumber;
    private String supplier;
    private String date;
    private String status;      // Pending | Received | Cancelled

    private Integer stock;
    private String itemName;
    private String category;


    private Double amount;
    private String createdDate;
    private String lastModifiedDate;
    private String externalId;

    // 🔗 Link back to Inventory item
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;
}
