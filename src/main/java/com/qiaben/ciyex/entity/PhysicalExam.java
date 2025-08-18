package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "physical_exam")
public class PhysicalExam {
    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;
    private Long encounterId;
    private Long orgId; // Organization ID for multi-tenancy

    @Lob
    private String general; // General findings

    @Lob
    private String heent; // HEENT findings

    @Lob
    private String neck; // Neck findings

    @Lob
    private String cardiovascular; // Cardiovascular findings

    @Lob
    private String respiratory; // Respiratory findings

    @Lob
    private String gastrointestinal; // Gastrointestinal findings

    @Lob
    private String musculoskeletal; // Musculoskeletal findings

    @Lob
    private String skin; // Skin findings

    @Lob
    private String lymphatic; // Lymphatic findings

    @Lob
    private String neurological; // Neurological findings

    @Lob
    private String psychiatric; // Psychiatric findings

    private String createdDate;
    private String lastModifiedDate;
}
