package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Invoice extends AuditableEntity {   // <-- public
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="external_id") private String externalId;    @Column(name="patient_id", nullable=false) private Long patientId;
    @Column(name="encounter_id", nullable=false) private Long encounterId;

    @Column(name="invoice_number") private String invoiceNumber;
    @Column(name="status") private String status;
    @Column(name="currency") private String currency;
    @Column(name="issue_date") private String issueDate;
    @Column(name="due_date") private String dueDate;
    @Column(name="payer") private String payer;
    @Column(name="notes", columnDefinition="TEXT") private String notes;

    @Column(name="total_gross", precision=18, scale=2) private BigDecimal totalGross;
    @Column(name="total_net", precision=18, scale=2) private BigDecimal totalNet;

    @OneToMany(mappedBy="invoice", cascade=CascadeType.ALL, orphanRemoval=true)
    @Builder.Default private List<InvoiceLine> lines = new ArrayList<>();

    @OneToMany(mappedBy="invoice", cascade=CascadeType.ALL, orphanRemoval=true)
    @Builder.Default private List<InvoicePayment> payments = new ArrayList<>();

    // audit fields provided by AuditableEntity

    // Backwards-compatible accessors for existing code that expects createdAt/updatedAt
    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
