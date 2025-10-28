package com.qiaben.ciyex.dto;

public class PatientCodeListDto {
    public Long id;     // echoed for completeness
    public String title;
    public Integer order;  // maps to entity.orderIndex
    public boolean isDefault;
    public boolean active;
    public String notes;
    public String codes;
}
