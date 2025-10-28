

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Entity
@Table(name = "procedure_item") // "procedure" is a reserved word in some DBs
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Procedure extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "encounter_id", nullable = false)
    private Long encounterId;

    @Column(name = "cpt4", length = 16)
    private String cpt4;

    @Column(name = "description", length = 1024)
    private String description;

    private Integer units;

    @Column(name = "rate", length = 64)
    private String rate; // change to BigDecimal if you want arithmetic ops

    @Column(name = "related_icds", length = 512)
    private String relatedIcds;

    @Column(name = "hb_start", length = 32)
    private String hospitalBillingStart;

    @Column(name = "hb_end", length = 32)
    private String hospitalBillingEnd;

    @Column(name = "modifier1", length = 10)
    private String modifier1;

    @Column(name = "modifier2", length = 10)
    private String modifier2;

    @Column(name = "modifier3", length = 10)
    private String modifier3;

    @Column(name = "modifier4", length = 10)
    private String modifier4;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    // audit fields provided by AuditableEntity

    // Backwards-compatible accessors for code that expects createdAt/updatedAt
    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }

    @Column(name = "price_level_id")
    private Integer priceLevelId;
    @Column(name = "price_level_title",  columnDefinition = "TEXT")
    private String priceLevelTitle;

    @Column(name = "provider_name")
    private String  providername;
//
//    @Column(name = "date_of_service")
//    private LocalDate dateOfService;
}
