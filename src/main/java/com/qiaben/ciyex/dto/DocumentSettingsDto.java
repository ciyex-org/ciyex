package com.qiaben.ciyex.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentSettingsDto {
    private int maxUploadSizeMB;
    private boolean enableAudio;
    private List<String> allowedFileTypes;
    private boolean encryptionEnabled;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Category {
        private String name;
        private boolean active;
    }

    private List<Category> categories;
}
