package org.ciyex.ehr.controller;

import org.ciyex.ehr.dto.integration.GpsConfig;
import org.ciyex.ehr.dto.integration.RequestContext;
import org.ciyex.ehr.util.OrgIntegrationConfigProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GpsController {

    private final OrgIntegrationConfigProvider configProvider;

    public GpsController(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @GetMapping("/api/gps/config")
    public Map<String, String> getGpsConfig() {
        try {


            GpsConfig gpsConfig = configProvider.getGpsForCurrentOrg();
            Map<String, String> config = new HashMap<>();
            
            if (gpsConfig != null) {
                config.put("collectjsPublicKey", gpsConfig.getCollectjsPublicKey());
                config.put("transactUrl", gpsConfig.getTransactUrl());
            }
            
            return config;
        } finally {
            RequestContext.clear();
        }
    }
}