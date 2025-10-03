package com.qiaben.ciyex.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
// OrgConfig should not hard-code a tenant schema so it can live in each tenant
// schema (practice_{id}). The application sets the connection's search_path
// to the correct tenant before repository operations.
@Table(name = "org_config")
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
