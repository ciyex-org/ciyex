package com.qiaben.ciyex.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "list_options")
@Data
public class ListOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "org_id", nullable = false)
    private String orgId;

    @Column(name = "list_id", nullable = false)
    private String listId;

    @Column(name = "option_id", nullable = false)
    private String optionId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "seq", nullable = false)
    private Integer seq;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "option_value")
    private Float optionValue;

    @Column(name = "notes")
    private String notes;

    @Column(name = "codes")
    private String codes;

    @Column(name = "activity")
    private Integer activity;

    @Column(name = "edit_options", nullable = false)
    private Boolean editOptions;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

}
