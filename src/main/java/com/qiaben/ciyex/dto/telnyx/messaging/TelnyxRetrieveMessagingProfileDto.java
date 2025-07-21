package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxRetrieveMessagingProfileDto {
    private String recordType;
    private String id;
    private String name;
    private boolean enabled;
    private String webhookUrl;
    private String webhookFailoverUrl;
    private String webhookApiVersion;
    private List<String> whitelistedDestinations;
    private String createdAt;
    private String updatedAt;
    private NumberPoolSettings numberPoolSettings;
    private UrlShortenerSettings urlShortenerSettings;
    private String v1Secret;

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
