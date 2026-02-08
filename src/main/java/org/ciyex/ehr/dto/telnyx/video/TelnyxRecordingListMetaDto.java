package org.ciyex.ehr.dto.telnyx.video;

import lombok.Data;

@Data
public class TelnyxRecordingListMetaDto {
    private Integer totalPages;
    private Integer totalResults;
    private Integer pageNumber;
    private Integer pageSize;
}
