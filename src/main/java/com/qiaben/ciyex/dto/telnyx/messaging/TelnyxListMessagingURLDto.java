package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;
import java.util.List;

@Data
public class TelnyxListMessagingURLDto {
    private List<MessagingUrlDomain> data;
    private Meta meta;

    @Data
    public static class MessagingUrlDomain {
        private String recordType;
        private String id;
        private String urlDomain;
        private String useCase;
    }

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
