package org.ciyex.ehr.lab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Entity @Table(name = "lab_order_set")
@Builder @NoArgsConstructor @AllArgsConstructor
public class LabOrderSet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String code;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "JSONB")
    private String tests; // JSON array of test definitions
    private String category;
    private Boolean active;
    private String orgAlias;
    private LocalDateTime createdAt;
}
