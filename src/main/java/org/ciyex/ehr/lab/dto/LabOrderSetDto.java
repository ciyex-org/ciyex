package org.ciyex.ehr.lab.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LabOrderSetDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String tests; // JSON string
    private String category;
    private Boolean active;
}
