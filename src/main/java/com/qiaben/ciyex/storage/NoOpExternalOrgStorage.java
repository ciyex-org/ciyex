package com.qiaben.ciyex.storage;

import com.qiaben.ciyex.entity.Location;
import com.qiaben.ciyex.entity.Org;
import org.springframework.stereotype.Component;

@Component("noOpExternalOrgStorage")
public class NoOpExternalOrgStorage implements ExternalOrgStorage {
    @Override
    public String create(Org localOrg) {
        return null;
    }

    @Override
    public void update(Org localOrg, String externalId) {
    }

    @Override
    public Org get(String externalId) {
        return null;
    }

    @Override
    public void delete(String externalId) {
    }

    @Override
    public String createLocation(Location location) {
        return null;
    }

    @Override
    public void updateLocation(Location location, String externalId) {
    }

    @Override
    public Location getLocation(String externalId) {
        return null;
    }

    @Override
    public void deleteLocation(String externalId) {
    }
}