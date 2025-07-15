package com.qiaben.ciyex.dto.telnyx;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetaDto {
    @JsonProperty("total_pages") private Integer totalPages;
    @JsonProperty("total_results") private Integer totalResults;
    @JsonProperty("page_number") private Integer pageNumber;
    @JsonProperty("page_size") private Integer pageSize;
}

