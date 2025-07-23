package com.qiaben.ciyex.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.qiaben.ciyex.util.JsonNodeConverter;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "org_config")
@Data
public class OrgConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // PK for this config row

    @Column(name = "org_id", nullable = false, unique = true)
    private Long orgId; // FK to orgs table, but not a relation object

    @Column(columnDefinition = "json")
    @Convert(converter = JsonNodeConverter.class)
    private JsonNode integrations;
}
