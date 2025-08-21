package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {   // <-- public
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="external_id") private String externalId;
    @Column(name="org_id", nullable=false) private Long orgId;
    @Column(name="patient_id", nullable=false) private Long patientId;
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

    @CreationTimestamp @Column(name="created_at", nullable=false, updatable=false)
    private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="updated_at", nullable=false)
    private LocalDateTime updatedAt;
}
