package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orgId;
    private String name;
    private String contact;
    private String phone;
    private String email;

    private String createdDate;
    private String lastModifiedDate;

    private String externalId;
}
