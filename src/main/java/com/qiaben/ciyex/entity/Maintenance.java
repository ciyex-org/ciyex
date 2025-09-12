package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Maintenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;

    private String equipment;
    private String category;
    private String location;

    private String dueDate;
    private String lastServiceDate;

    private String assignee;
    private String vendor;
    private String priority;
    private String status;

    @Column(length = 2000)
    private String notes;

    private String createdDate;
    private String lastModifiedDate;

    private String externalId;
}
