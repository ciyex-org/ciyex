package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "communications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Communication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "org_id", nullable = false)
    private Long orgId;

    private String status;
    private String category;
    private String sentDate;
    private String createdDate;
    private String lastModifiedDate;

    private String sender;
    private String recipients;
    private String subject;
    private String inResponseTo;
}