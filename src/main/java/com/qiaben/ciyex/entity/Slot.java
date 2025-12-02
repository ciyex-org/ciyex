package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "slots")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Slot extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_id", nullable = false)
    private Long providerId;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "start_time")
    private String start;

    @Column(name = "end_time")
    private String end;

    @Column(name = "status")
    private String status;

    @Column(name = "comment")
    private String comment;

}
