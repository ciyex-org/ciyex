package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxUpdateMessagingProfileDto {
    private String name;
    private boolean enabled;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private String webhookApiVersion;
    private List<String> whitelistedDestinations;
    private String v1Secret;
    private NumberPoolSettings numberPoolSettings;
    private UrlShortenerSettings urlShortenerSettings;
    private String alphaSender;
    private String dailySpendLimit;
    private boolean dailySpendLimitEnabled;
    private boolean mmsFallBackToSms;
    private boolean mmsTranscoding;

    @Data
    public static class NumberPoolSettings {
        private int tollFreeWeight;
        private int longCodeWeight;
        private boolean skipUnhealthy;
        private boolean stickySender;
        private boolean geomatch;
    }

    @Data
    public static class UrlShortenerSettings {
        private String domain;
        private String prefix;
        private boolean replaceBlacklistOnly;
        private boolean sendWebhooks;
    }
}
