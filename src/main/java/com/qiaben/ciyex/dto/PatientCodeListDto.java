package com.qiaben.ciyex.dto;



public class PatientCodeListDto {
    public Long id;
    public Long orgId;     // echoed for completeness
    public String title;
    public Integer order;  // maps to entity.orderIndex
    public boolean isDefault;
    public boolean active;
    public String notes;
    public String codes;
}
