package com.qiaben.ciyex.service;

import com.qiaben.ciyex.entity.Org;
import com.qiaben.ciyex.repository.OrgRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrgService {

    @Autowired
    private OrgRepository orgRepository;

    /**
     * Get all orgs
     */
    public List<Org> getAllOrgs() {
        return orgRepository.findAll();
    }

    /**
     * Get org by ID
     */
    public Optional<Org> getOrgById(Long id) {
        return orgRepository.findById(id);
    }

    /**
     * Get org by Name
     */
    public Optional<Org> getOrgByName(String orgName) {
        return orgRepository.findByOrgName(orgName);
    }

    /**
     * Create new org (fails if orgName exists)
     */
    public Org createOrg(Org org) {
        if (orgRepository.findByOrgName(org.getOrgName()).isPresent()) {
            throw new RuntimeException("Organization with this name already exists");
        }
        return orgRepository.save(org);
    }

    /**
     * Update org by ID
     */
    public Org updateOrg(Long id, Org updatedOrg) {
        return orgRepository.findById(id)
                .map(org -> {
                    org.setOrgName(updatedOrg.getOrgName());
                    org.setAddress(updatedOrg.getAddress());
                    org.setCity(updatedOrg.getCity());
                    org.setState(updatedOrg.getState());
                    org.setPostalCode(updatedOrg.getPostalCode());
                    org.setCountry(updatedOrg.getCountry());
                    // Don't update users directly here
                    return orgRepository.save(org);
                })
                .orElseThrow(() -> new RuntimeException("Organization not found"));
    }

    /**
     * Delete org by ID
     */
    public void deleteOrg(Long id) {
        orgRepository.deleteById(id);
    }
}
