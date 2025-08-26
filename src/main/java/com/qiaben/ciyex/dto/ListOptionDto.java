package com.qiaben.ciyex.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class ListOptionDto {

    private Long id;
    private String orgId;
    private String listId;
    private String optionId;
    private String title;
    private Integer seq;
    private Boolean isDefault;
    private Float optionValue;
    private String notes;
    private String codes;
    private Integer activity;
    private Boolean editOptions;
    private LocalDateTime timestamp;
    private LocalDateTime lastUpdated;

}
