package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "slots")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "external_id")
    private String externalId;

    private String createdDate;
    private String lastModifiedDate;
}
