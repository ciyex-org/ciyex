package org.ciyex.ehr.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxMessagingOptOutResponse {
    private List<TelnyxMessagingOptOutDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
