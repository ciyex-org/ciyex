package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EqualsAndHashCode(callSuper = true)
public class Maintenance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    // audit fields provided by AuditableEntity

    private String externalId;
}
