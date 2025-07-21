package com.qiaben.ciyex.service.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxBrandFeedbackResponseDto {
    private String brandId;
    private List<Category> category;

    @Data
    public static class Category {
        private String id;
        private String displayName;
        private String description;
        private List<String> fields;
    }
}
