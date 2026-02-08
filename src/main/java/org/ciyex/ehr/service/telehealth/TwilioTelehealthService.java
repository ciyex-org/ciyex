package org.ciyex.ehr.service.telehealth;

import org.ciyex.ehr.dto.integration.IntegrationKey;

import org.ciyex.ehr.util.TenantContextUtil;
import org.ciyex.ehr.dto.integration.TelehealthConfig;
import org.ciyex.ehr.util.OrgIntegrationConfigProvider;

import com.twilio.Twilio;
import com.twilio.rest.video.v1.Room;
import com.twilio.rest.video.v1.RoomCreator;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VideoGrant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@TelehealthVendor("twilio")
@Component
@Slf4j
public class TwilioTelehealthService implements TelehealthService {

    private final OrgIntegrationConfigProvider configProvider;

    @Autowired
    public TwilioTelehealthService(OrgIntegrationConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    @Override
    public String startVideoCall(Long providerId, Long patientId, String roomName) {
        String tenantName = TenantContextUtil.getTenantName();
        TelehealthConfig cfg = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        ensureTwilioConfigured(cfg);

        String accountSid = cfg.getTwilio().getAccountSid();
        String authToken  = cfg.getTwilio().getAuthToken();

        Twilio.init(accountSid, authToken);

        String uniqueName = roomName + "-" + (tenantName != null ? tenantName : "default");

        Room room = new RoomCreator()
                .setUniqueName(uniqueName)
                .setType(Room.RoomType.GROUP)
                // .setRecordParticipantsOnConnect(true) // optional
                .create();

        log.info("Started Twilio video call for tenant={}, roomSid={}, uniqueName={}", tenantName, room.getSid(), uniqueName);
        return room.getSid();
    }

    /**
     * Mint a Twilio Video access token (JWT) so a client can join the room.
     */
    public String createJoinToken(String roomName, String identity, Integer ttlSecs) {
        TelehealthConfig cfg = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        ensureTwilioConfigured(cfg);

        TelehealthConfig.Twilio t = cfg.getTwilio();
        if (isBlank(t.getAccountSid()) || isBlank(t.getApiKeySid()) || isBlank(t.getApiKeySecret())) {
            throw new IllegalStateException("Twilio accountSid, apiKeySid, and apiKeySecret are required to mint join tokens.");
        }

        int ttl = (ttlSecs != null && ttlSecs > 0) ? ttlSecs : 3600;

        VideoGrant grant = new VideoGrant().setRoom(roomName);

        // IMPORTANT: this Twilio SDK signature expects (String, String, byte[])
        byte[] secretBytes = t.getApiKeySecret().getBytes(StandardCharsets.UTF_8);

        AccessToken token = new AccessToken.Builder(
                t.getAccountSid(),
                t.getApiKeySid(),   // String
                secretBytes         // byte[]
        )
                .identity(identity)
                .ttl(ttl)
                .grant(grant)
                .build();

        return token.toJwt();
    }

    @Override
    public String getCallStatus(String callId) {
        TelehealthConfig cfg = configProvider.getForCurrentTenant(IntegrationKey.TELEHEALTH);
        ensureTwilioConfigured(cfg);

        String accountSid = cfg.getTwilio().getAccountSid();
        String authToken  = cfg.getTwilio().getAuthToken();
        Twilio.init(accountSid, authToken);

        Room room = Room.fetcher(callId).fetch();
        return room.getStatus().toString();
    }

    public JoinTokenWithRoom ensureRoomAndCreateToken(Long providerId, Long patientId, String roomName, String identity, Integer ttlSecs) {
        String roomSid = startVideoCall(providerId, patientId, roomName);
        String tenantName = TenantContextUtil.getTenantName();
        String uniqueName = roomName + "-" + (tenantName != null ? tenantName : "default");
        String token = createJoinToken(uniqueName, identity, ttlSecs);
        return new JoinTokenWithRoom(roomSid, identity, token);
    }

    public record JoinTokenWithRoom(String roomSid, String identity, String token) {}

    // ----------------- helpers -----------------

    private static void ensureTwilioConfigured(TelehealthConfig cfg) {
        if (cfg == null || cfg.getTwilio() == null) {
            throw new IllegalStateException("Telehealth Twilio configuration is missing.");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
