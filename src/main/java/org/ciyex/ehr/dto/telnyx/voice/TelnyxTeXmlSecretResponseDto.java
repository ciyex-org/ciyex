package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;

@Data
public class TelnyxTeXmlSecretResponseDto {
    private DataObject data;

    @Data
    public static class DataObject {
        private String name;
        private String value;
    }
}
