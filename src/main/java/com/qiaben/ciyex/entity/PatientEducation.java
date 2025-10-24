package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PatientEducation {
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

    private String createdDate;
    private String lastModifiedDate;

    private String externalId;
}
