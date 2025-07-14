package com.qiaben.ciyex.dto.fhir;

import lombok.Data;

@Data
public class FhirDocumentReferenceDTO {
    private String id;
    private String resourceType;
    private Meta meta;
    private Identifier[] identifier;
    private String status;
    private Type type;
    private Category[] category;
    private Subject subject;
    private String date;
    private Author[] author;
    private Content[] content;

    @Data
    public static class Meta {
        private String lastUpdated;
    }

    @Data
    public static class Identifier {
        private String value;
    }

    @Data
    public static class Type {
        private Coding[] coding;
    }

    @Data
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }

    @Data
    public static class Category {
        private Coding[] coding;
    }

    @Data
    public static class Subject {
        private String reference;
        private String type;
    }

    @Data
    public static class Author {
        private String reference;
    }

    @Data
    public static class Content {
        private Attachment attachment;
    }

    @Data
    public static class Attachment {
        private String contentType;
        private String url;
    }
}
