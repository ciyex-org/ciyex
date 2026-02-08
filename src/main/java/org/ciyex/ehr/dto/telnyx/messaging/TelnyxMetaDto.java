package org.ciyex.ehr.dto.telnyx.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelnyxMetaDto {
    @JsonProperty("total_pages") private Integer totalPages;
    @JsonProperty("total_results") private Integer totalResults;
    @JsonProperty("page_number") private Integer pageNumber;
    @JsonProperty("page_size") private Integer pageSize;
}

