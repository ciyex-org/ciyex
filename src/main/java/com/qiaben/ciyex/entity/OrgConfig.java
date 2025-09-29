package com.qiaben.ciyex.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "org_config",schema = "practice_2")
@Data
public class OrgConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;    // PK for this config row

    @Column(name = "org_id", nullable = false, unique = true)
    private Long orgId; // FK to orgs table, but not a relation object

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode integrations;
}
