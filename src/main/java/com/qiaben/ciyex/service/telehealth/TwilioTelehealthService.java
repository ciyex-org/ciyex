package com.qiaben.ciyex.service.telehealth;

import com.qiaben.ciyex.dto.integration.IntegrationKey;
import com.qiaben.ciyex.dto.integration.TelehealthConfig;
import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
import com.qiaben.ciyex.dto.integration.RequestContext;
import com.twilio.Twilio;
import com.twilio.rest.video.v1.Room;
import com.twilio.rest.video.v1.RoomCreator;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        String accountSid = config.getTwilio().getAccountSid();
        String authToken = config.getTwilio().getAuthToken();
        Twilio.init(accountSid, authToken);
        Room room = new RoomCreator()
                .setUniqueName(roomName + "-" + orgId)
                .setType(Room.RoomType.GROUP)
                .create();
        log.info("Started Twilio video call for orgId: {}, roomSid: {}", orgId, room.getSid());
        return room.getSid();
    }

    @Override
    public void sendSMSReminder(String phoneNumber, String message) {
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        String accountSid = config.getTwilio().getAccountSid();
        String authToken = config.getTwilio().getAuthToken();
        String messagingServiceSid = config.getTwilio().getMessagingServiceSid();
        Twilio.init(accountSid, authToken);
        Message.creator(
                new PhoneNumber(phoneNumber),
                messagingServiceSid,
                message
        ).create();
        log.info("Sent SMS reminder for orgId: {}", orgId);
    }

    @Override
    public String getCallStatus(String callId) {
        Long orgId = RequestContext.get().getOrgId();
        TelehealthConfig config = configProvider.get(orgId, IntegrationKey.TELEHEALTH);
        String accountSid = config.getTwilio().getAccountSid();
        String authToken = config.getTwilio().getAuthToken();
        Twilio.init(accountSid, authToken);
        Room room = Room.fetcher(callId).fetch();
        return room.getStatus().toString();
    }
}