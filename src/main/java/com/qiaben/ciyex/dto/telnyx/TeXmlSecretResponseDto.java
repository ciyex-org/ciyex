package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class TeXmlSecretResponseDto {
    private DataObject data;

    @Data
    public static class DataObject {
        private String name;
        private String value;
    }
}
