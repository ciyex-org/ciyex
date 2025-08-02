package com.qiaben.ciyex.service.core;


import com.qiaben.ciyex.entity.Org;

public interface ExternalOrgStorage {
    String create(Org localOrg);
    void update(Org localOrg, String externalId);
    Org get(String externalId); // Returns local-mapped Org for sync if needed
    void delete(String externalId);
}
