package com.qiaben.ciyex.model;

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

    private String defaultPrice;

    @Column(nullable = false)
    private String createdDate;

    @Column(nullable = false)
    private String lastModifiedDate;
}
