package com.qiaben.ciyex.service;


import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    public OrgConfigService(OrgConfigRepository orgConfigRepository) {
        this.orgConfigRepository = orgConfigRepository;
    }

    /**
     * Ensures we're in the master schema (public) before executing OrgConfig operations
     */
    private void ensureMasterSchema() {
        try {
            entityManager.createNativeQuery("SET search_path TO public").executeUpdate();
            log.debug("Set schema to public for OrgConfig operations");
        } catch (Exception e) {
            log.warn("Failed to set schema to public for OrgConfig operations: {}", e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<OrgConfig> findAll() {
        ensureMasterSchema();
        return orgConfigRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<OrgConfig> findById(Long id) {
        ensureMasterSchema();
        return orgConfigRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<OrgConfig> findByOrgId(Long orgId) {
        ensureMasterSchema();
        return orgConfigRepository.findByOrgId(orgId);
    }

    @Transactional
    public OrgConfig create(OrgConfig orgConfig) {
        ensureMasterSchema();
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
        ensureMasterSchema();
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
        ensureMasterSchema();
        if (!orgConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("OrgConfig not found with id: " + id);
        }
        orgConfigRepository.deleteById(id);
    }
}
