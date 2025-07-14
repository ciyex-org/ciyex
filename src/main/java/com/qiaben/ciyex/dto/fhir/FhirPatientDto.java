package com.qiaben.ciyex.dto.fhir;

import lombok.Data;
import java.util.List;

@Data
public class FhirPatientDto {
    private String id;
    private MetaDto meta;
    private String resourceType;
    private List<IdentifierDto> identifier;
    private Boolean active;
    private List<NameDto> name;
    private String gender;
    private String birthDate;
    private List<AddressDto> address;
    private List<CommunicationDto> communication;
    // add other fields as needed

    @Data
    public static class MetaDto {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    public static class IdentifierDto {
        private String use;
        private TypeDto type;
        private String system;
        private String value;

        @Data
        public static class TypeDto {
            private List<CodingDto> coding;

            @Data
            public static class CodingDto {
                private String system;
                private String code;
            }
        }
    }

    @Data
    public static class NameDto {
        private String use;
        private String family;
        private List<String> given;
    }

    @Data
    public static class AddressDto {
        private List<String> line;
        private String city;
        private String state;
        private String postalCode;
        private PeriodDto period;

        @Data
        public static class PeriodDto {
            private String start;
        }
    }

    @Data
    public static class CommunicationDto {
        private LanguageDto language;

        @Data
        public static class LanguageDto {
            private List<CodingDto> coding;

            @Data
            public static class CodingDto {
                private String system;
                private String code;
                private String display;
            }
        }
    }
}
