package com.qiaben.ciyex.dto.telnyx.voice;

import lombok.Data;

import java.util.List;

@Data
public class TelnyxAutoResponseListResponse {
    private List<TelnyxAutoResponseDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int totalPages;
        private int totalResults;
        private int pageNumber;
        private int pageSize;
    }
}
