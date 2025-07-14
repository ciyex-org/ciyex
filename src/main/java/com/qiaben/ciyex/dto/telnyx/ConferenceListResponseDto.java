package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ConferenceListResponseDto {

    private List<ConferenceResponseDto.DataDto> data;
    private MetaDto meta;

    @Data
    public static class MetaDto {
        @JsonProperty("total_pages")
        private int totalPages;
        @JsonProperty("total_results")
        private int totalResults;
        @JsonProperty("page_number")
        private int pageNumber;
        @JsonProperty("page_size")
        private int pageSize;
    }
}
