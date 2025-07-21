package com.qiaben.ciyex.dto.telnyx.messaging;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxRcsAgentListResponse {
    private List<TelnyxRcsAgentDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
