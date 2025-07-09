package com.qiaben.ciyex.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrganizationRequestDTO {
    private String resourceType = "Organization";
    private List<Identifier> identifier;
    private Boolean active;
    private List<Type> type;
    private String name;
    private List<Telecom> telecom;
    private List<Address> address;

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
        private String line;
        private String city;
        private String state;
        private String postalCode;
    }
}
