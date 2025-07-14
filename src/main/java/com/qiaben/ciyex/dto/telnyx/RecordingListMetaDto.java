package com.qiaben.ciyex.dto.telnyx;

import lombok.Data;

@Data
public class RecordingListMetaDto {
    private Integer totalPages;
    private Integer totalResults;
    private Integer pageNumber;
    private Integer pageSize;
}
