package org.ciyex.ehr.dto.telnyx.voice;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxTextToSpeechDto {

    @Data
    public static class VoiceInfo {
        private String id;
        private String name;
        private String provider;
        private String label;
        private String accent;
        private String gender;
        private String age;
        private String language;
    }

    @Data
    public static class VoiceListResponse {
        private List<VoiceInfo> voices;
    }

    @Data
    public static class GenerateSpeechRequest {
        private String voice;
        private String text;
    }
}
