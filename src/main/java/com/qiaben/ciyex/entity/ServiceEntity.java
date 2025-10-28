package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "default_price")
    private String defaultPrice;

    @Column(name = "created_date", nullable = false)
    private String createdDate;

    @Column(name = "last_modified_date", nullable = false)
    private String lastModifiedDate;
}
