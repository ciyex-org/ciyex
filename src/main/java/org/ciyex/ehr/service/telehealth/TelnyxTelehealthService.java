package org.ciyex.ehr.service.telehealth;

import org.ciyex.ehr.dto.integration.IntegrationKey;
import org.ciyex.ehr.dto.integration.TelehealthConfig;
import org.ciyex.ehr.util.OrgIntegrationConfigProvider;
import org.ciyex.ehr.util.TenantContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@TelehealthVendor("telnyx")
@Component
@Slf4j
public class TelnyxTelehealthService implements TelehealthService {

    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public TelnyxTelehealthService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public String startVideoCall(Long providerId, Long patientId, String roomName) {
    String tenantName = TenantContextUtil.getTenantName();
    TelehealthConfig config = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        String apiKey = config.getTelnyx().getApiKey();
       
        //TODO: Implement actual video call start logic with Telnyx
        return null;
    }

    @Override
    public String getCallStatus(String callId) {
    String tenantName = TenantContextUtil.getTenantName();
    TelehealthConfig config = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        String apiKey = config.getTelnyx().getApiKey();
        // Placeholder; Telnyx call status requires call control API integration
    log.info("Get call status for tenant: {}, callId: {}", tenantName, callId);
        return "active"; // Example
    }

    @Override
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        return "";
    }
}