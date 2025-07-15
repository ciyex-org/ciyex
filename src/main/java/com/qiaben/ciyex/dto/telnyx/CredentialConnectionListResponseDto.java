package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class CredentialConnectionListResponseDto {
    private List<CredentialConnectionResponseDto> data;
    private Meta meta;

    @Data
    public static class Meta {
        private int total_pages;
        private int total_results;
        private int page_number;
        private int page_size;
    }
}
