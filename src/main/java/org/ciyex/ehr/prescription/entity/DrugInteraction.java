package org.ciyex.ehr.prescription.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "drug_interaction")
@Builder @NoArgsConstructor @AllArgsConstructor
public class DrugInteraction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "drug_a_code")
    private String drugACode;
    @Column(name = "drug_a_name")
    private String drugAName;
    @Column(name = "drug_b_code")
    private String drugBCode;
    @Column(name = "drug_b_name")
    private String drugBName;
    private String severity;       // minor, moderate, major, contraindicated
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "clinical_effect", columnDefinition = "TEXT")
    private String clinicalEffect;
    @Column(columnDefinition = "TEXT")
    private String management;
    private String source;
    @Column(name = "org_alias")
    private String orgAlias;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
