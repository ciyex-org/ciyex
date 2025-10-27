package com.qiaben.ciyex.entity;

import com.qiaben.ciyex.entity.TemplateContext;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "template_document",
        indexes = {
                @Index(name = "idx_tpldoc_org", columnList = "org_id"),
                @Index(name = "idx_tpldoc_org_context", columnList = "org_id,context"),
                @Index(name = "idx_tpldoc_org_name", columnList = "org_id,name")
        })
public class TemplateDocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false)
    private Long id;

    /** Owning organization */
    @Column(name = "org_id", nullable = false)
    private Long orgId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TemplateContext context;

    @Column(nullable = false, length = 300)
    private String name;

    /** Full HTML (can be thousands of lines) */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Front-end options blob, stored as Postgres JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> options;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // Getters & setters
    public Long getId() { return id; }

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }

    public TemplateContext getContext() { return context; }
    public void setContext(TemplateContext context) { this.context = context; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Map<String, Object> getOptions() { return options; }
    public void setOptions(Map<String, Object> options) { this.options = options; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}


