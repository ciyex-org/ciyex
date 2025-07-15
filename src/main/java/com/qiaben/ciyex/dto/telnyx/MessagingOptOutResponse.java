package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

import java.util.List;

@Data
public class MessagingOptOutResponse {
    private List<MessagingOptOutDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
