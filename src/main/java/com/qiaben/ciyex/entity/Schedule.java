package com.qiaben.ciyex.entity;


import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "provider_id", nullable = false)
    private Long providerId;


    


    @Column(name = "external_id")
    private String externalId;


    // local audit (optional but commonly present in your codebase)
    private String createdDate;
    private String lastModifiedDate;
}