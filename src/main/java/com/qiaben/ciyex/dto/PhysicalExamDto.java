package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class PhysicalExamDto {

    private Long id;
    private Long patientId;
    private Long encounterId;
    private Long orgId;

    private String general;
    private String heent;

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    private String neck;
    private String cardiovascular;
    private String respiratory;
    private String gastrointestinal;
    private String musculoskeletal;
    private String skin;
    private String lymphatic;
    private String neurological;
    private String psychiatric;

    private String createdDate;
    private String lastModifiedDate;
}
