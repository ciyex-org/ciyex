package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class InsuranceCompanyDto {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String payerId;

    private String country;
    private String fhirId;
    private String status;  // ACTIVE or ARCHIVED

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
    private Audit audit;
}
