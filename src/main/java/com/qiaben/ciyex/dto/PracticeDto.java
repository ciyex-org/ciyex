package com.qiaben.ciyex.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PracticeDto {

    private Long id;
    private String name;
    private String description;
    private String externalId;
    private String fhirId;

    // Session timeout in minutes (5-30)
    private Integer tokenExpiryMinutes;

    // Practice Settings
    private PracticeSettings practiceSettings;

    // Regional Settings
    private RegionalSettings regionalSettings;

    // Contact Information
    private Contact contact;

    // Audit Information
    private Audit audit;

    /**
     * Get token expiry minutes (session timeout)
     */
    public Integer getTokenExpiryMinutes() {
        return tokenExpiryMinutes;
    }

    /**
     * Set token expiry minutes (session timeout)
     * Valid range: 5-30 minutes
     */
    public void setTokenExpiryMinutes(Integer tokenExpiryMinutes) {
        if (tokenExpiryMinutes != null && tokenExpiryMinutes >= 5 && tokenExpiryMinutes <= 30) {
            this.tokenExpiryMinutes = tokenExpiryMinutes;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PracticeSettings {
        private Boolean enablePatientPractice;
        private Integer sessionTimeoutMinutes;
        private Integer tokenExpiryMinutes;

        public Integer getTokenExpiryMinutes() {
            return tokenExpiryMinutes;
        }

        public void setTokenExpiryMinutes(Integer tokenExpiryMinutes) {
            this.tokenExpiryMinutes = tokenExpiryMinutes;
            this.sessionTimeoutMinutes = tokenExpiryMinutes; // Keep both in sync
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RegionalSettings {
        private String unitsForVisitForms; // US, Metric, Both
        private String displayFormatUSWeights; // Show pounds as decimal value, Show pounds and ounces
        private String telephoneCountryCode;
        private String dateDisplayFormat; // YYYY-MM-DD, MM/DD/YYYY, DD/MM/YYYY
        private String timeDisplayFormat; // 24 hr, 12 hr
        private String timeZone;
        private String currencyDesignator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Contact {
        private String email;
        private String phoneNumber;
        private String faxNumber;
        private Address address;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class Address {
            private String line1;
            private String line2;
            private String city;
            private String state;
            private String postalCode;
            private String country;

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                if (line1 != null) sb.append(line1);
                if (line2 != null && !line2.trim().isEmpty()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(line2);
                }
                if (city != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(city);
                }
                if (state != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(state);
                }
                if (postalCode != null) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(postalCode);
                }
                if (country != null) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(country);
                }
                return sb.toString();
            }
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Audit {
        private String createdDate;
        private String lastModifiedDate;
        private String createdBy;
        private String lastModifiedBy;
    }
}