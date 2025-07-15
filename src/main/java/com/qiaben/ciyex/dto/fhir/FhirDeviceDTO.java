package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class FhirDeviceDTO {

    private String id;
    private String meta;
    private String resourceType;
    private String udiCarrier;
    private String distinctIdentifier;
    private String manufactureDate;
    private String expirationDate;
    private String lotNumber;
    private String serialNumber;
    private String type;
    private Patient patient;

    @Data
    public static class Patient {
        private String reference;
        private String type;
    }
}
