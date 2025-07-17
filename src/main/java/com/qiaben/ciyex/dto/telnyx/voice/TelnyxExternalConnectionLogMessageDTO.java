package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxExternalConnectionLogMessageDTO {
    private List<LogMessage> log_messages;
    private Meta meta;

    @Data
    public static class LogMessage {
        private Integer code;
        private String title;
        private String detail;
        private Source source;
    }

    @Data
    public static class Source {
        private String pointer;
        private String parameter;
        private String header;
    }

    @Data
    public static class Meta {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }
}
