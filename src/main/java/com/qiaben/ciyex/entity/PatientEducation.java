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
public class PatientEducation extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String title;
    private String summary;
    private String category;
    private String language;
    private String readingLevel;

    @Column(length = 5000)
    private String content;

    // audit fields provided by AuditableEntity

    private String externalId;
}
