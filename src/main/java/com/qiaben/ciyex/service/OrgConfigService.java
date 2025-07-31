package com.qiaben.ciyex.service;


import com.qiaben.ciyex.entity.OrgConfig;
import com.qiaben.ciyex.repository.OrgConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrgConfigService {

    private final OrgConfigRepository orgConfigRepository;

    public OrgConfigService(OrgConfigRepository orgConfigRepository) {
        this.orgConfigRepository = orgConfigRepository;
    }

    public List<OrgConfig> findAll() {
        return orgConfigRepository.findAll();
    }

    public Optional<OrgConfig> findById(Long id) {
        return orgConfigRepository.findById(id);
    }

    public Optional<OrgConfig> findByOrgId(Long orgId) {
        return orgConfigRepository.findByOrgId(orgId);
    }

    public OrgConfig create(OrgConfig orgConfig) {
        if (orgConfig.getOrgId() == null) {
            throw new IllegalArgumentException("orgId cannot be null");
        }
        if (orgConfigRepository.findByOrgId(orgConfig.getOrgId()).isPresent()) {
            throw new IllegalArgumentException("OrgConfig with this orgId already exists");
        }
        return orgConfigRepository.save(orgConfig);
    }

    public OrgConfig update(Long id, OrgConfig updatedOrgConfig) {
        return orgConfigRepository.findById(id)
                .map(orgConfig -> {
                    orgConfig.setIntegrations(updatedOrgConfig.getIntegrations());
                    // orgId is unique, optionally allow update or skip
                    orgConfig.setOrgId(updatedOrgConfig.getOrgId());
                    return orgConfigRepository.save(orgConfig);
                })
                .orElseThrow(() -> new IllegalArgumentException("OrgConfig not found with id: " + id));
    }

    public void delete(Long id) {
        if (!orgConfigRepository.existsById(id)) {
            throw new IllegalArgumentException("OrgConfig not found with id: " + id);
        }
        orgConfigRepository.deleteById(id);
    }
}
