package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class ObservationResponseDTO {
    private Object meta;
    private String resourceType;
    private String type;
    private Integer total;
    private Object[] link;
    private Object[] entry;
}
