package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name="invoice_payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InvoicePayment {   // <-- public
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="invoice_id", nullable=false)
    private Invoice invoice;

    @Column(name="pay_date")                  private String date;
    @Column(name="amount", precision=18,scale=2) private BigDecimal amount;
    @Column(name="method", length=32)         private String method;
    @Column(name="reference", length=64)      private String reference;
    @Column(name="note", columnDefinition="TEXT") private String note;
}
