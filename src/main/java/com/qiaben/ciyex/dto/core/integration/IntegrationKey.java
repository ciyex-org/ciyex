package com.qiaben.ciyex.dto.core.integration;

public enum IntegrationKey {
    OPENEMR("openemr", OpenEmrConfig.class),
    STRIPE("stripe", StripeConfig.class),
    SPHERE("sphere", SphereConfig.class),
    TWILIO("twilio", TwilioConfig.class),
    SMTP("smtp", SmtpConfig.class);

    private final String key;
    private final Class<?> clazz;

    IntegrationKey(String key, Class<?> clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    public String key() { return key; }
    public Class<?> clazz() { return clazz; }
}

