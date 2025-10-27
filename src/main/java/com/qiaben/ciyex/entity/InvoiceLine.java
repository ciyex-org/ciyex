package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Entity
@Table(name="invoice_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class InvoiceLine extends AuditableEntity {   // <-- public
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_id", nullable=false)
    private Invoice invoice;

    @Column(name="description", length=1024) private String description;
    @Column(name="code", length=20)          private String code;
    @Column(name="quantity")                 private Integer quantity;
    @Column(name="unit_price", precision=18, scale=2) private BigDecimal unitPrice;
    @Column(name="amount", precision=18, scale=2)     private BigDecimal amount;
}
