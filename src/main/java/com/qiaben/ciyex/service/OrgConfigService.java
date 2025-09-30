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
                    // orgId is unique, optionally allow update or skip
                    orgConfig.setOrgId(updatedOrgConfig.getOrgId());
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
}
