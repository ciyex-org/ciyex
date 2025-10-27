package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.qiaben.ciyex.dto.integration.RequestContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final DataSource dataSource;

    public OrgConfigService(OrgConfigRepository orgConfigRepository, DataSource dataSource) {
        this.orgConfigRepository = orgConfigRepository;
        this.dataSource = dataSource;
    }

    /**
     * Ensure the connection's search_path is set to the tenant schema for the given orgId.
     * This runs on a separate JDBC connection (autocommit) so it does not participate in
     * the caller's transaction and is not affected by an earlier aborted transaction.
     */
    private void ensureTenantSchema(Long orgId) {
        if (orgId == null) return;
        String schemaName = "practice_" + orgId;
        if (dataSource != null) {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public");
                log.debug("Set search_path to {} via DataSource for OrgConfig operations", schemaName);
                return;
            } catch (Exception e) {
                log.warn("Failed to set search_path to {} via DataSource: {}", schemaName, e.getMessage());
            }
        }

        // Fallback: use EntityManager (may be part of caller txn)
        try {
            entityManager.createNativeQuery("SET search_path TO " + com.qiaben.ciyex.util.SqlIdentifier.quote(schemaName) + ", public").executeUpdate();
            log.debug("Set search_path to {} via EntityManager (fallback)", schemaName);
        } catch (Exception e) {
            log.warn("Failed to set schema to {} for OrgConfig operations: {}", schemaName, e.getMessage());
        }
    }

    private void ensureTenantSchemaFromContext() {
        try {
            RequestContext ctx = RequestContext.get();
            if (ctx != null && ctx.getOrgId() != null) {
                ensureTenantSchema(ctx.getOrgId());
            }
        } catch (Exception e) {
            log.debug("No RequestContext available to set tenant schema: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<OrgConfig> findAll() {
        ensureTenantSchemaFromContext();
        return orgConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrgConfig> findById(Long id) {
        ensureTenantSchemaFromContext();
        return orgConfigRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<OrgConfig> findByOrgId(Long orgId) {
        ensureTenantSchema(orgId);
        return orgConfigRepository.findByOrgId(orgId);
    }

    @Transactional
    public OrgConfig create(OrgConfig orgConfig) {
        ensureTenantSchema(orgConfig.getOrgId());
        if (orgConfig.getOrgId() == null) {
            throw new IllegalArgumentException("orgId cannot be null");
        }
        if (orgConfigRepository.findByOrgId(orgConfig.getOrgId()).isPresent()) {
            throw new IllegalArgumentException("OrgConfig with this orgId already exists");
        }
        return orgConfigRepository.save(orgConfig);
    }

    @Transactional
    public OrgConfig update(Long id, OrgConfig updatedOrgConfig) {
        ensureTenantSchemaFromContext();
        return orgConfigRepository.findById(id)
                .map(orgConfig -> {
                    orgConfig.setIntegrations(updatedOrgConfig.getIntegrations());
                    // ⚠️ orgId should not usually change; better to skip updating it
                    return orgConfigRepository.save(orgConfig);
                })
                .orElseThrow(() -> new IllegalArgumentException("OrgConfig not found with id: " + id));
    }

    @Transactional
    public void delete(Long id) {
        ensureTenantSchemaFromContext();
        if (!orgConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("OrgConfig not found with id: " + id);
        }
        orgConfigRepository.deleteById(id);
    }

    /**
     * ✅ Fetch the Stripe secret key for the given orgId from OrgConfig.integrations JSON/Map.
     */
    @Transactional(readOnly = true)
    public String getStripeSecretKey(Long orgId) {
        return findByOrgId(orgId)
                .map(orgConfig -> {
                    if (orgConfig.getIntegrations() == null) return null;

                    // integrations is stored as a JsonNode (jsonb). Try common shapes:
                    // 1) { "stripe": { "apiKey": "sk_..." } }
                    // 2) { "stripeSecretKey": "sk_..." }
                    try {
                        com.fasterxml.jackson.databind.JsonNode integrations = orgConfig.getIntegrations();

                        com.fasterxml.jackson.databind.JsonNode stripeNode = integrations.get("stripe");
                        if (stripeNode != null && !stripeNode.isNull()) {
                            com.fasterxml.jackson.databind.JsonNode apiKey = stripeNode.get("apiKey");
                            if (apiKey != null && !apiKey.isNull()) return apiKey.asText();

                            com.fasterxml.jackson.databind.JsonNode secret = stripeNode.get("secret");
                            if (secret != null && !secret.isNull()) return secret.asText();
                        }

                        com.fasterxml.jackson.databind.JsonNode topLevel = integrations.get("stripeSecretKey");
                        if (topLevel != null && !topLevel.isNull()) return topLevel.asText();

                        // Last resort: look for apiKey at top-level
                        com.fasterxml.jackson.databind.JsonNode apiKeyTop = integrations.get("apiKey");
                        if (apiKeyTop != null && !apiKeyTop.isNull()) return apiKeyTop.asText();
                    } catch (Exception e) {
                        log.warn("Failed to extract Stripe key from integrations JSON for orgId={}: {}", orgId, e.getMessage());
                    }
                    return null;
                })
                .orElse(null);
    }
}
