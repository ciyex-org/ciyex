    package org.ciyex.ehr.service.notification;

    import org.ciyex.ehr.service.OrgConfigService;

    import com.twilio.Twilio;
    import com.twilio.rest.api.v2010.account.Message;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;

    @Service
    @Slf4j
    public class SmsNotificationService {

        private final OrgConfigService orgConfigService;

        public SmsNotificationService(OrgConfigService orgConfigService) {
            this.orgConfigService = orgConfigService;
        }

        public void sendSms(String to, String body) {
            String accountSid = orgConfigService.getConfig("twilio.accountSid").orElse("");
            String authToken = orgConfigService.getConfig("twilio.authToken").orElse("");
            String fromNumber = orgConfigService.getConfig("twilio.phoneNumber").orElse("");

            if (accountSid.isEmpty() || authToken.isEmpty() || fromNumber.isEmpty()) {
                log.error("Twilio config missing in org_config table");
                throw new RuntimeException("Twilio config missing");
            }

            Twilio.init(accountSid, authToken);

            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(fromNumber),
                    body
            ).create();

            log.info("✅ SMS sent to {} with SID {}", to, message.getSid());
        }
    }
