



package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class SocialHistoryEntryDto {
    private Long id;          // may be null for new rows
    private String category;  // e.g., Tobacco, Alcohol, Occupation
    private String details;   // free text
    private String value;     // e.g., "Never", "Former", "Daily"
}
