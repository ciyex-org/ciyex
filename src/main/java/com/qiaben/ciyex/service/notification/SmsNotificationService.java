    package com.qiaben.ciyex.service.notification;

    import com.qiaben.ciyex.dto.integration.IntegrationKey;
    import com.qiaben.ciyex.dto.integration.TwilioConfig;
    import com.qiaben.ciyex.util.OrgIntegrationConfigProvider;
    import com.qiaben.ciyex.util.TenantContextUtil;

    import com.twilio.Twilio;
    import com.twilio.rest.api.v2010.account.Message;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Service;

    @Service
    @Slf4j
    public class SmsNotificationService {

        private final OrgIntegrationConfigProvider configProvider;

        public SmsNotificationService(OrgIntegrationConfigProvider configProvider) {
            this.configProvider = configProvider;
        }

        public void sendSms(String to, String body) {
            String tenantName = TenantContextUtil.getTenantName();
            TwilioConfig twilio = configProvider.getForCurrentTenant(IntegrationKey.TWILIO);
            if (tenantName == null) {
                log.warn("SMS send attempted with no tenantName in context");
            }

            Twilio.init(twilio.getAccountSid(), twilio.getAuthToken());

            Message message = Message.creator(
                    new com.twilio.type.PhoneNumber(to),
                    new com.twilio.type.PhoneNumber(twilio.getPhoneNumber()),
                    body
            ).create();

            log.info("✅ SMS sent to {} with SID {}", to, message.getSid());
        }
    }
