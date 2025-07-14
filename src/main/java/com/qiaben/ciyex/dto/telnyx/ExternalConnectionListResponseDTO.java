package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;
import java.util.List;

@Data
public class ExternalConnectionListResponseDTO {
    private List<ExternalConnectionDTO> data;
    private Meta meta;

    @Data
    public static class Meta {
        private Integer total_pages;
        private Integer total_results;
        private Integer page_number;
        private Integer page_size;
    }
}
