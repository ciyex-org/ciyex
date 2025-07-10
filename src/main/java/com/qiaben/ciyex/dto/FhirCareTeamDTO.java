package com.qiaben.ciyex.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FhirCareTeamDTO {
    private String id;
    private MetaDTO meta;
    private String resourceType;
    private String status;
    private SubjectDTO subject;
    private ParticipantDTO[] participant;

    @Data
    @NoArgsConstructor
    public static class MetaDTO {
        private String versionId;
        private String lastUpdated;
    }

    @Data
    @NoArgsConstructor
    public static class SubjectDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class ParticipantDTO {
        private RoleDTO[] role;
        private MemberDTO member;
        private OnBehalfOfDTO onBehalfOf;
    }

    @Data
    @NoArgsConstructor
    public static class RoleDTO {
        private CodingDTO[] coding;
    }

    @Data
    @NoArgsConstructor
    public static class CodingDTO {
        private String system;
        private String code;
        private String display;
    }

    @Data
    @NoArgsConstructor
    public static class MemberDTO {
        private String reference;
        private String type;
    }

    @Data
    @NoArgsConstructor
    public static class OnBehalfOfDTO {
        private String reference;
        private String type;
    }
}
