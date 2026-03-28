package org.ciyex.ehr.dto.integration;

public enum IntegrationKey {
    PRACTICE_DB("practice_db", PracticeDbConfig.class),
    FHIR("fhir", FhirConfig.class),
    STRIPE("stripe", StripeConfig.class),
    GPS("gps", GpsConfig.class),
    SPHERE("sphere", SphereConfig.class),
    TWILIO("twilio", TwilioConfig.class),
    SMTP("smtp", SmtpConfig.class),
    TELEHEALTH("telehealth", TelehealthConfig.class),
    AI("ai", AiConfig.class),
    DOCUMENT_STORAGE("document_storage", StorageConfig.class);

    private final String key;
    private final Class<?> clazz;

    IntegrationKey(String key, Class<?> clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    public String key() { return key; }
    public Class<?> clazz() { return clazz; }
}

