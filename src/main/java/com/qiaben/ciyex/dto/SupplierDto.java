package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class SupplierDto {
    private Long id;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String createdDate;
    private String lastModifiedDate;
    private String externalId;

    private Audit audit;

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
