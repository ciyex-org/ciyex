// src/main/java/com/ciyex/ciyex/dto/UpdateAddressRequest.java
package org.ciyex.ehr.dto;

import lombok.Data;

@Data
public class UpdateAddressRequest {
    private String street;
    private String street2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String email; // used to identify user (or you may use userId)
}
