package com.qiaben.ciyex.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {
    private String externalId; // ID from external storage
    private String name;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
