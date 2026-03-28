package org.ciyex.ehr.dto.portal;

import lombok.Data;
import java.time.LocalDate;
import java.time.Instant;

@Data
public class PortalDemographicsDto {
    private Long id;
    private String fhirId;

    // Audit
    private Audit audit;

    // Identity
    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private String sex;
    private String maritalStatus;

    // Contact
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneMobile;
    private String contactEmail;

    // Emergency
    private String emergencyContactName;
    private String emergencyContactPhone;

    // Preferences
    private boolean allowSMS;
    private boolean allowEmail;
    private boolean allowVoiceMessage;
    private boolean allowMailMessage;

    @Data
    public static class Audit {
        private Instant createdDate;
        private Instant lastModifiedDate;
    }
}
