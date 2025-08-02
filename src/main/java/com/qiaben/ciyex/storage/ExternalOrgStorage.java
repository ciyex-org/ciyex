package com.qiaben.ciyex.storage;


import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.entity.Org;

public interface ExternalOrgStorage {
    String create(Org localOrg);
    void update(Org localOrg, String externalId);
    Org get(String externalId); // Returns local-mapped Org for sync if needed
    void delete(String externalId);

    // Location methods
    String createLocation(Location location);
    void updateLocation(Location location, String externalId);
    Location getLocation(String externalId);
    void deleteLocation(String externalId);
}
