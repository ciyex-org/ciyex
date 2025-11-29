//package com.qiaben.ciyex.entity;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.JdbcTypeCode;
//import org.hibernate.type.SqlTypes;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "plan")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class Plan {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    
//
//
//    @Column(name = "patient_id", nullable = false)
//    private Long patientId;
//
//    @Column(name = "encounter_id", nullable = false)
//    private Long encounterId;
//
//    @Column(name = "diagnostic_plan")
//    private String diagnosticPlan;
//
//    // DB column is plan_text; API uses "plan" in DTO
//    @Column(name = "plan_text")
//    private String plan;
//
//    @Column(name = "notes")
//    private String notes;
//
//    @Column(name = "follow_up_visit")
//    private String followUpVisit;
//
//    @Column(name = "return_work_school")
//    private String returnWorkSchool;
//
//    /**
//     * Store as JSONB in PostgreSQL.
//     * Hibernate 6 can handle JSON via @JdbcTypeCode(SqlTypes.JSON).
//     */
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "sections_json", columnDefinition = "jsonb")
//    private JsonNode sectionsJson;
//
//    public JsonNode getSectionsJson() {
//        return sectionsJson;
//    }
//
//    public void setSectionsJson(JsonNode sectionsJson) {
//        this.sectionsJson = sectionsJson;
//    }
//
//    // Convenience overload: allow existing code calling setSectionsJson(String)
//    public void setSectionsJson(String json) {
//        if (json == null || json.isBlank()) {
//            this.sectionsJson = null;
//            return;
//        }
//        try {
//            this.sectionsJson = new ObjectMapper().readTree(json);
//        } catch (Exception e) {
//            throw new IllegalArgumentException("Invalid JSON for sections_json", e);
//        }
//    }
//
//    @Column(name = "external_id")
//    private String externalId;
//
//    @Column(name = "created_at", nullable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at", nullable = false)
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    public void onCreate() {
//        LocalDateTime now = LocalDateTime.now();
//        this.createdAt = now;
//        this.updatedAt = now;
//    }
//
//    @PreUpdate
//    public void onUpdate() {
//        this.updatedAt = LocalDateTime.now();
//    }
//}

package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "plan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(callSuper = true)
public class Plan extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "patient_id", nullable = false) private Long patientId;
    @Column(name = "encounter_id", nullable = false) private Long encounterId;

    // NOTE: keep everything as strings/text — including sectionsJson
    @Column(name = "diagnostic_plan", columnDefinition = "text") private String diagnosticPlan;
    @Column(name = "plan",            columnDefinition = "text") private String plan;
    @Column(name = "notes",           columnDefinition = "text") private String notes;
    @Column(name = "follow_up_visit", length = 255)              private String followUpVisit;
    @Column(name = "return_work_school", length = 255)           private String returnWorkSchool;
    @Column(name = "sections_json",   columnDefinition = "text") private String sectionsJson;

    @Column(name = "external_id", length = 255) private String externalId;
    @Column(name = "fhir_id", length = 255) private String fhirId;

    // eSign / Print
    @Builder.Default
    @Column(name = "e_signed")         private Boolean eSigned = Boolean.FALSE;
    @Column(name = "signed_at")        private OffsetDateTime signedAt;
    @Column(name = "signed_by", length = 128) private String signedBy;
    @Column(name = "printed_at")       private OffsetDateTime printedAt;

    // audit fields provided by AuditableEntity

    public LocalDateTime getCreatedAt() { return getCreatedDate(); }
    public void setCreatedAt(LocalDateTime createdAt) { setCreatedDate(createdAt); }
    public LocalDateTime getUpdatedAt() { return getLastModifiedDate(); }
    public void setUpdatedAt(LocalDateTime updatedAt) { setLastModifiedDate(updatedAt); }
}
