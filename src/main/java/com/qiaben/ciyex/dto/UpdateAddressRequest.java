// src/main/java/com/qiaben/ciyex/dto/UpdateAddressRequest.java
package com.qiaben.ciyex.dto;

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
