package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ServicebillDto {
    private Long id;
    private String name;
    private String defaultPrice;

    private Audit audit = new Audit();

    @Data
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
    }
}
