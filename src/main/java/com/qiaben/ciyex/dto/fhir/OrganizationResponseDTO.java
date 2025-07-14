package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import java.util.List;

@Data
public class OrganizationResponseDTO {
    private String id;
    private Meta meta;
    private String resourceType = "Organization";
    private Text text;
    private List<Identifier> identifier;
    private Boolean active;
    private List<Type> type;
    private String name;
    private List<Telecom> telecom;
    private List<Address> address;

    @Data
    public static class Meta {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    public static class Text {
        private String status;
        private String div;
    }

    @Data
    public static class Identifier {
        private String system;
        private String value;
    }

    @Data
    public static class Type {
        private List<Coding> coding;
    }

    @Data
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }

    @Data
    public static class Telecom {
        private String system;
        private String value;
        private String use;
    }

    @Data
    public static class Address {
        // You can further define the address structure if needed
        private String line;
        private String city;
        private String state;
        private String postalCode;
    }
}
