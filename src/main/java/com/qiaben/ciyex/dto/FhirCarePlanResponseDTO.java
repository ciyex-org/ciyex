package com.qiaben.ciyex.dto;

import lombok.Data;

@Data
public class FhirCarePlanResponseDTO {
    private String id;
    private Meta meta;
    private String resourceType;
    private Text text;
    private String status;
    private String intent;
    private Category[] category;

    public void setId(String id) {
        this.id = id;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setCategory(Category[] category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    private String description;
    private Subject subject;

    public String getId() {
        return id;
    }

    public Meta getMeta() {
        return meta;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Text getText() {
        return text;
    }

    public String getStatus() {
        return status;
    }

    public String getIntent() {
        return intent;
    }

    public Category[] getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public Subject getSubject() {
        return subject;
    }

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
    public static class Category {
        private Coding[] coding;
    }

    @Data
    public static class Coding {
        private String system;
        private String code;
        private String display;
    }

    @Data
    public static class Subject {
        private String reference;
        private String type;
    }
}